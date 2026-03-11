package com.example.demo.core;

import cn.hutool.core.util.IdUtil;
import com.example.demo.dto.ApiCaseDTO;
import com.example.demo.dto.ReportDTO;
import com.example.demo.service.ResultSenderService;
import com.example.demo.util.CustomFileUtil;
import com.example.demo.util.JMeterUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.jmeter.config.CSVDataSet;
import org.apache.jmeter.engine.StandardJMeterEngine;
import org.apache.jmeter.reporters.Summariser;
import org.apache.jmeter.save.SaveService;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.collections.HashTree;
import org.apache.jorphan.collections.SearchByClass;
import org.springframework.context.ApplicationContext;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Objects;

/**
 * JMeter执行引擎基类
 * 定义了执行流程和一些公共方法，具体的测试计划由子类进行实现
 *
 * @author eddy
 */
@Data
@Slf4j
public abstract class BaseJMeterEngine {

    /**
     * 最终的测试计划
     */
    private HashTree testPlanHashTree;

    /**
     * 测试引擎
     */
    protected StandardJMeterEngine engine;

    /**
     * 测试用例
     */
    protected ApiCaseDTO apiCaseDTO;

    /**
     * 测试报告
     */
    protected ReportDTO reportDTO;

    /**
     * spring的应用上下文
     */
    protected ApplicationContext applicationContext;

    /**
     * 开始任务
     * 模版方法，具体的由子类进行实现
     */
    public void startTest() {
        // 初始化测试引擎
        this.initStressEngine();
        // 组装测试计划 抽象方法
        this.assembleTestPlan();
        // 方便调试使用，可以不用
        this.hashTree2Jmx();
        // 运行测试
        this.run();
        // 运行完用例后，清理相关的资源
        this.clearData();
        // 更新测试报告
        this.updateReport();
    }

    /**
     * 获取结果收集器
     *
     * @param resultSenderService 结果发送服务
     * @return 结果收集器
     */
    public EngineSampleCollector getEngineSampleCollector(ResultSenderService resultSenderService) {
        // Summariser对象
        Summariser summer = null;
        // Summariser名称
        String summariserName = JMeterUtils.getPropDefault("summariser.name", "summary");
        if (!summariserName.isEmpty()) {
            // 创建Summariser对象
            summer = new Summariser(summariserName);
        }
        // 使用自定义结果收集器
        EngineSampleCollector collector = new EngineSampleCollector(apiCaseDTO, summer, resultSenderService, reportDTO);
        // 如果要调整收集器名称
        collector.setName(apiCaseDTO.getName());
        collector.setEnabled(Boolean.TRUE);
        return collector;
    }

    /**
     * 更新测试报告
     */
    public void updateReport() {
        while (!engine.isActive()) {
//            ReportFeignService reportFeignService = applicationContext.getBean(ReportFeignService.class);
//            ReportUpdateReq reportUpdateReq = ReportUpdateReq.builder()
//                    .id(reportDTO.getId())
//                    .executeState(ReportStateEnum.COUNTING_REPORT.name())
//                    .endTime(System.currentTimeMillis()).build();
//            reportFeignService.update(reportUpdateReq);
//            break;
        }
    }

    /**
     * 清理相关资源文件
     */
    public void clearData() {
        // 寻找JMX里面的CSVDataSet
        SearchByClass<TestElement> testElementVisitor = new SearchByClass<>(TestElement.class);
        testPlanHashTree.traverse(testElementVisitor);
        Collection<TestElement> searchResults = testElementVisitor.getSearchResults();
        // 提取里面的csv data set的类，获取filename路径，然后删除
        for (TestElement testElement : searchResults) {
            if (testElement instanceof CSVDataSet csvDataSet) {
                String filename = csvDataSet.getProperty("filename").getStringValue();
                Path path = Paths.get(filename);
                try {
                    Files.delete(path);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    /**
     * 运行压测
     */
    public void run() {
        if (Objects.nonNull(testPlanHashTree)) {
            engine.configure(testPlanHashTree);
            // 运行引擎
            engine.run();
        }
    }

    /**
     * 把测试计划转为本地JMX文件
     */
    public void hashTree2Jmx() {
        try {
            JMeterUtil.initJmeterProperties();
            SaveService.loadProperties();
            String dir = System.getProperty("user.dir") + File.separator + "static" + File.separator;
            CustomFileUtil.mkdir(dir);
            String localJmxPath = dir + IdUtil.simpleUUID() + ".jmx";
            SaveService.saveTree(testPlanHashTree, new FileOutputStream(localJmxPath));
        } catch (Exception e) {
            e.printStackTrace();
            log.error("保存本地jmx失败");
        }
    }

    /**
     * 组装测试计划，交给子类进行实现
     */
    public abstract void assembleTestPlan();

    /**
     * 初始化测试引擎
     */
    public void initStressEngine() {
        engine = JMeterUtil.getJmeterEngine();
    }

}
