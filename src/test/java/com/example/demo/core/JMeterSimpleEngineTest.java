package com.example.demo.core;

import com.example.demo.dto.*;
import com.example.demo.enums.AssertActionEnum;
import com.example.demo.enums.AssertSourceEnum;
import com.example.demo.enums.ProcessorTypeEnum;
import com.example.demo.enums.ScriptLanguageEnum;
import com.example.demo.service.impl.RedisResultSenderServiceImpl;
import org.apache.jmeter.assertions.ResponseAssertion;
import org.apache.jmeter.extractor.JSR223PostProcessor;
import org.apache.jmeter.modifiers.JSR223PreProcessor;
import org.apache.jmeter.protocol.http.control.HeaderManager;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerProxy;
import org.apache.jmeter.timers.ConstantTimer;
import com.example.demo.util.JMeterUtil;
import org.apache.jmeter.save.SaveService;
import org.apache.jmeter.testelement.TestPlan;
import org.apache.jmeter.threads.ThreadGroup;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.collections.HashTree;
import org.apache.jorphan.collections.SearchByClass;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpMethod;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * JMeterSimpleEngine 单元测试
 * 验证测试计划的组装逻辑，并将生成的脚本保存为 JMX 文件供手动查验
 */
class JMeterSimpleEngineTest {

    // ---------------------------------------------------------------
    // 初始化 JMeter 环境（只需执行一次）
    // ---------------------------------------------------------------

    @BeforeAll
    static void initJMeter() throws Exception {
        System.setProperty("java.awt.headless", "true");

        // 测试环境没有真实的 JMeter 安装目录，创建临时目录提供最小化 jmeter.properties
        File tempHome = Files.createTempDirectory("jmeter-home-test").toFile();
        File binDir = new File(tempHome, "bin");
        binDir.mkdirs();
        File propsFile = new File(binDir, "jmeter.properties");
        try (java.io.FileWriter fw = new java.io.FileWriter(propsFile)) {
            fw.write("# minimal test props\n");
        }

        // ① loadJMeterProperties 会重置 appProperties，必须最先执行
        JMeterUtils.loadJMeterProperties(propsFile.getAbsolutePath());
        JMeterUtils.setJMeterHome(tempHome.getAbsolutePath());
        JMeterUtils.setProperty("sampleresult.default.encoding", "UTF-8");
        JMeterUtils.initLocale();
        // ② 加载 saveservice.properties 并注册 XStream 别名（复用正式代码）
        JMeterUtil.loadSaveServiceProperties();
    }

    // ---------------------------------------------------------------
    // 辅助方法：构造测试数据
    // ---------------------------------------------------------------

    /** 构造一个包含全局请求头的环境 */
    private EnvironmentDTO buildEnvironment() {
        ParamDTO contentType = new ParamDTO();
        contentType.setName("Content-Type");
        contentType.setValue("application/json");
        contentType.setEnabled(true);

        ParamDTO token = new ParamDTO();
        token.setName("Authorization");
        token.setValue("Bearer test-token");
        token.setEnabled(true);

        EnvironmentHttpInfoDTO httpInfo = new EnvironmentHttpInfoDTO();
        httpInfo.setProtocol("http");
        httpInfo.setDomain("httpbin.org");
        httpInfo.setHeaders(List.of(contentType, token));

        EnvironmentDTO env = new EnvironmentDTO();
        env.setId(1L);
        env.setName("测试环境");
        env.setHttpInfos(List.of(httpInfo));
        return env;
    }

    /** 构造一个包含 GET + POST 两个步骤的用例 */
    private ApiCaseDTO buildApiCase() {
        // ---------- GET 步骤：带 Query 参数 + 响应码断言 ----------
        ApiCaseHttpStepDTO getStep = new ApiCaseHttpStepDTO();
        getStep.setName("GET /get");
        getStep.setMethod(HttpMethod.GET);
        getStep.setPath("/get");

        ParamDTO queryParam = new ParamDTO();
        queryParam.setName("foo");
        queryParam.setValue("bar");
        getStep.setQuery(List.of(queryParam));

        AssertionDTO codeAssertion = new AssertionDTO();
        codeAssertion.setName("响应码等于 200");
        codeAssertion.setAction(AssertActionEnum.EQUAL);
        codeAssertion.setSource(AssertSourceEnum.RESPONSE_CODE);
        codeAssertion.setValue("200");

        AssertionDTO bodyAssertion = new AssertionDTO();
        bodyAssertion.setName("响应体包含 url");
        bodyAssertion.setAction(AssertActionEnum.CONTAIN);
        bodyAssertion.setSource(AssertSourceEnum.RESPONSE_DATA);
        bodyAssertion.setValue("url");

        getStep.setAssertion(List.of(codeAssertion, bodyAssertion));

        // 步骤级请求头
        ParamDTO stepHeader = new ParamDTO();
        stepHeader.setName("X-Custom-Step");
        stepHeader.setValue("step-get");
        getStep.setHeader(List.of(stepHeader));

        // ---------- POST 步骤：无额外断言 ----------
        ApiCaseHttpStepDTO postStep = new ApiCaseHttpStepDTO();
        postStep.setName("POST /post");
        postStep.setMethod(HttpMethod.POST);
        postStep.setPath("/post");

        // ---------- 组装用例 ----------
        ApiCaseDTO apiCase = new ApiCaseDTO();
        apiCase.setId(1L);
        apiCase.setName("httpbin 接口测试用例");
        apiCase.setSteps(List.of(getStep, postStep));
        return apiCase;
    }

    /** 构造测试报告 */
    private ReportDTO buildReport() {
        return ReportDTO.builder()
                .id(100L)
                .name("单测报告")
                .build();
    }

    /** 构造 Mock 的 ApplicationContext */
    private ApplicationContext buildMockContext() {
        ApplicationContext ctx = mock(ApplicationContext.class);
        RedisResultSenderServiceImpl sender = mock(RedisResultSenderServiceImpl.class);
        when(ctx.getBean(RedisResultSenderServiceImpl.class)).thenReturn(sender);
        return ctx;
    }

    /** 构造包含前置脚本、前置定时器、后置脚本的用例 */
    private ApiCaseDTO buildApiCaseWithProcessors() {
        // 前置脚本处理器
        ScriptProcessorDTO preScript = new ScriptProcessorDTO();
        preScript.setLanguage(ScriptLanguageEnum.GROOVY);
        preScript.setContent("log.info('pre-script')");
        ProcessorDTO preScriptProcessor = new ProcessorDTO();
        preScriptProcessor.setProcessorType(ProcessorTypeEnum.SCRIPT);
        preScriptProcessor.setScript(preScript);

        // 前置定时器
        TimerProcessorDTO timerDTO = new TimerProcessorDTO();
        timerDTO.setWaitTime(500L);
        ProcessorDTO timerProcessor = new ProcessorDTO();
        timerProcessor.setProcessorType(ProcessorTypeEnum.TIMER);
        timerProcessor.setTimer(timerDTO);

        // 后置脚本处理器
        ScriptProcessorDTO postScript = new ScriptProcessorDTO();
        postScript.setLanguage(ScriptLanguageEnum.BEANSHELL);
        postScript.setContent("log.info('post-script')");
        ProcessorDTO postScriptProcessor = new ProcessorDTO();
        postScriptProcessor.setProcessorType(ProcessorTypeEnum.SCRIPT);
        postScriptProcessor.setScript(postScript);

        ApiCaseHttpStepDTO step = new ApiCaseHttpStepDTO();
        step.setName("GET /processor-test");
        step.setMethod(HttpMethod.GET);
        step.setPath("/processor-test");
        step.setPreProcessor(List.of(preScriptProcessor, timerProcessor));
        step.setPostProcessor(List.of(postScriptProcessor));

        ApiCaseDTO apiCase = new ApiCaseDTO();
        apiCase.setId(2L);
        apiCase.setName("处理器测试用例");
        apiCase.setSteps(List.of(step));
        return apiCase;
    }

    // ---------------------------------------------------------------
    // 测试方法
    // ---------------------------------------------------------------

    @Test
    @DisplayName("组装测试计划 - 验证 TestPlan / ThreadGroup / HTTPSampler 数量")
    void testAssembleTestPlan_structureCheck() {
        JMeterSimpleEngine engine = new JMeterSimpleEngine(
                buildEnvironment(), buildApiCase(), buildReport(), buildMockContext());

        engine.assembleTestPlan();

        HashTree tree = engine.getTestPlanHashTree();
        assertNotNull(tree, "testPlanHashTree 不应为 null");

        // 验证 TestPlan
        SearchByClass<TestPlan> tpSearch = new SearchByClass<>(TestPlan.class);
        tree.traverse(tpSearch);
        Collection<TestPlan> testPlans = tpSearch.getSearchResults();
        assertEquals(1, testPlans.size(), "应存在 1 个 TestPlan");
        assertEquals("httpbin 接口测试用例", testPlans.iterator().next().getName(), "TestPlan 名称应与用例名称一致");

        // 验证 ThreadGroup
        SearchByClass<ThreadGroup> tgSearch = new SearchByClass<>(ThreadGroup.class);
        tree.traverse(tgSearch);
        assertEquals(1, tgSearch.getSearchResults().size(), "应存在 1 个 ThreadGroup");

        // 验证 HTTP 采样器数量 = 步骤数（GET + POST）
        SearchByClass<HTTPSamplerProxy> samplerSearch = new SearchByClass<>(HTTPSamplerProxy.class);
        tree.traverse(samplerSearch);
        Collection<HTTPSamplerProxy> samplers = samplerSearch.getSearchResults();
        assertEquals(2, samplers.size(), "应存在 2 个 HTTPSampler");
    }

    @Test
    @DisplayName("组装测试计划 - 验证 GET 步骤的采样器属性与断言")
    void testAssembleTestPlan_getSamplerDetail() {
        JMeterSimpleEngine engine = new JMeterSimpleEngine(
                buildEnvironment(), buildApiCase(), buildReport(), buildMockContext());

        engine.assembleTestPlan();
        HashTree tree = engine.getTestPlanHashTree();

        // 找到所有 HTTP 采样器，定位 GET 那一个
        SearchByClass<HTTPSamplerProxy> samplerSearch = new SearchByClass<>(HTTPSamplerProxy.class);
        tree.traverse(samplerSearch);
        HTTPSamplerProxy getSampler = samplerSearch.getSearchResults().stream()
                .filter(s -> "GET /get".equals(s.getName()))
                .findFirst()
                .orElse(null);

        assertNotNull(getSampler, "应能找到名为 'GET /get' 的采样器");
        assertEquals("GET", getSampler.getMethod(), "请求方法应为 GET");
        assertEquals("/get", getSampler.getPath(), "请求路径应为 /get");
        // GET 步骤的 Query 参数应被添加到 Arguments 里
        assertEquals("bar", getSampler.getArguments().getArgument(0).getValue(),
                "Query 参数 foo 的值应为 bar");

        // 验证断言数量（两条断言：响应码 + 响应体）
        SearchByClass<ResponseAssertion> assertionSearch = new SearchByClass<>(ResponseAssertion.class);
        tree.traverse(assertionSearch);
        assertEquals(2, assertionSearch.getSearchResults().size(), "应存在 2 个 ResponseAssertion");
    }

    @Test
    @DisplayName("组装测试计划 - 验证环境级 HeaderManager 被加入线程组")
    void testAssembleTestPlan_envHeaderManager() {
        JMeterSimpleEngine engine = new JMeterSimpleEngine(
                buildEnvironment(), buildApiCase(), buildReport(), buildMockContext());

        engine.assembleTestPlan();
        HashTree tree = engine.getTestPlanHashTree();

        SearchByClass<HeaderManager> headerSearch = new SearchByClass<>(HeaderManager.class);
        tree.traverse(headerSearch);
        // 环境级 1 个 + GET 步骤级 1 个 = 共 2 个 HeaderManager
        assertEquals(2, headerSearch.getSearchResults().size(), "应存在 2 个 HeaderManager（环境级 + 步骤级）");
    }

    @Test
    @DisplayName("组装测试计划 - 将脚本保存为 JMX 文件供手动查阅")
    void testAssembleTestPlan_saveJmx() throws Exception {
        JMeterSimpleEngine engine = new JMeterSimpleEngine(
                buildEnvironment(), buildApiCase(), buildReport(), buildMockContext());

        engine.assembleTestPlan();
        HashTree tree = engine.getTestPlanHashTree();
        assertNotNull(tree);

        // 保存 JMX 到临时目录，路径打印到控制台便于查阅
        String jmxPath = System.getProperty("java.io.tmpdir")
                + File.separator + "jmeter-simple-engine-test.jmx";
        SaveService.saveTree(tree, new FileOutputStream(jmxPath));
        System.out.println("\n========================================");
        System.out.println("JMX 文件已保存：" + jmxPath);
        System.out.println("========================================\n");

        // 验证文件存在且不为空
        File jmxFile = new File(jmxPath);
        assertTrue(jmxFile.exists(), "JMX 文件应被成功创建");
        assertTrue(jmxFile.length() > 0, "JMX 文件内容不应为空");

        // ★ 关键断言：根节点必须是 <jmeterTestPlan，而非全类名 <org.apache.jmeter.save.ScriptWrapper
        // 若 SaveService.loadProperties() 未在 saveTree() 之前调用，
        // XStream 别名不会注册，根节点会被写成全类名，JMeter GUI 打开时报
        // "CannotResolveClassException: version" 错误。
        String jmxContent = java.nio.file.Files.readString(jmxFile.toPath());
        assertTrue(jmxContent.contains("<jmeterTestPlan"),
                "JMX 根节点应为 <jmeterTestPlan>，实际内容开头：\n" + jmxContent.substring(0, Math.min(300, jmxContent.length())));
        assertFalse(jmxContent.contains("org.apache.jmeter.save.ScriptWrapper"),
                "JMX 不应包含未别名化的全类名 ScriptWrapper，否则 JMeter GUI 将无法解析");
    }

    @Test
    @DisplayName("前置处理器 - 脚本类型应生成 JSR223PreProcessor")
    void testAssembleTestPlan_preProcessor_script() {
        JMeterSimpleEngine engine = new JMeterSimpleEngine(
                buildEnvironment(), buildApiCaseWithProcessors(), buildReport(), buildMockContext());
        engine.assembleTestPlan();
        HashTree tree = engine.getTestPlanHashTree();

        SearchByClass<JSR223PreProcessor> search = new SearchByClass<>(JSR223PreProcessor.class);
        tree.traverse(search);
        Collection<JSR223PreProcessor> result = search.getSearchResults();

        assertEquals(1, result.size(), "应存在 1 个 JSR223PreProcessor");
        JSR223PreProcessor pre = result.iterator().next();
        assertEquals("groovy", pre.getPropertyAsString("scriptLanguage"), "脚本语言应为 groovy");
        assertEquals("log.info('pre-script')", pre.getPropertyAsString("script"), "脚本内容应一致");
    }

    @Test
    @DisplayName("前置处理器 - 定时器类型应生成 ConstantTimer")
    void testAssembleTestPlan_preProcessor_timer() {
        JMeterSimpleEngine engine = new JMeterSimpleEngine(
                buildEnvironment(), buildApiCaseWithProcessors(), buildReport(), buildMockContext());
        engine.assembleTestPlan();
        HashTree tree = engine.getTestPlanHashTree();

        SearchByClass<ConstantTimer> search = new SearchByClass<>(ConstantTimer.class);
        tree.traverse(search);
        Collection<ConstantTimer> result = search.getSearchResults();

        assertEquals(1, result.size(), "应存在 1 个 ConstantTimer");
        assertEquals("500", result.iterator().next().getDelay(), "等待时长应为 500ms");
    }

    @Test
    @DisplayName("后置处理器 - 脚本类型应生成 JSR223PostProcessor")
    void testAssembleTestPlan_postProcessor_script() {
        JMeterSimpleEngine engine = new JMeterSimpleEngine(
                buildEnvironment(), buildApiCaseWithProcessors(), buildReport(), buildMockContext());
        engine.assembleTestPlan();
        HashTree tree = engine.getTestPlanHashTree();

        SearchByClass<JSR223PostProcessor> search = new SearchByClass<>(JSR223PostProcessor.class);
        tree.traverse(search);
        Collection<JSR223PostProcessor> result = search.getSearchResults();

        assertEquals(1, result.size(), "应存在 1 个 JSR223PostProcessor");
        JSR223PostProcessor post = result.iterator().next();
        assertEquals("beanshell", post.getPropertyAsString("scriptLanguage"), "脚本语言应为 beanshell");
        assertEquals("log.info('post-script')", post.getPropertyAsString("script"), "脚本内容应一致");
    }
}
