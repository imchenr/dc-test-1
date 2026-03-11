package com.example.demo.util;

import org.apache.jmeter.engine.StandardJMeterEngine;
import org.apache.jmeter.save.SaveService;
import org.apache.jmeter.util.JMeterUtils;

import java.io.File;
import java.net.URL;

/**
 * JMeter工具类
 *
 * @author eddy
 */
public class JMeterUtil {

    /**
     * 获取Jmeter的home路径，临时写法，后续部署上线会调整
     *
     * @return Jmeter的home路径
     */
    public static String getJmeterHome() {
        String osName = System.getProperty("os.name");
        try {
            if (osName.contains("Mac")) {
                return "/Users/eddy/apache-jmeter-5.6.3";
            } else {
                //生产环境
                return "/apache-jmeter-5.5";
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取JMeter bin目录
     *
     * @return JMeter bin目录
     */
    public static String getJmeterHomeBin() {
        return getJmeterHome() + File.separator + "bin";
    }

    /**
     * 初始化 JMeter 配置文件，并加载 XStream 序列化别名（saveservice.properties）。
     * <p>
     * saveservice.properties 打包在 classpath 中（src/main/resources），
     * 通过 saveservice_properties 属性覆盖默认的文件系统路径，
     * 确保在无独立 JMeter 安装目录的环境（容器、CI）下也能正常序列化 JMX 文件。
     */
    public static void initJmeterProperties() {
        String jmeterHome = getJmeterHome();
        String jmeterHomeBin = getJmeterHomeBin();
        // ① 加载 jmeter.properties（此操作会重置 appProperties，后续 set 必须在此之后执行）
        JMeterUtils.loadJMeterProperties(jmeterHomeBin + File.separator + "jmeter.properties");
        // ② 设置 jmeter 安装目录
        JMeterUtils.setJMeterHome(jmeterHome);
        // ③ 避免中文响应乱码
        JMeterUtils.setProperty("sampleresult.default.encoding", "UTF-8");
        // ④ 初始化本地环境
        JMeterUtils.initLocale();
        // ⑤ 加载 XStream 序列化别名（必须在 loadJMeterProperties 之后）
        loadSaveServiceProperties();
    }

    /**
     * 从 classpath 加载 saveservice.properties，注册 XStream 序列化别名。
     * <p>
     * 需在 {@link JMeterUtils#loadJMeterProperties} 之后调用，
     * 否则 saveservice_properties 属性会被重置。
     */
    public static void loadSaveServiceProperties() {
        URL url = JMeterUtil.class.getClassLoader().getResource("saveservice.properties");
        if (url == null) {
            throw new IllegalStateException(
                    "saveservice.properties 不在 classpath 中，请确认 src/main/resources/ 下存在该文件");
        }
        try {
            // 绝对路径写入属性，JMeterUtils.findFile() 检测到绝对路径后直接使用，跳过 jmeter.home 拼接
            JMeterUtils.setProperty("saveservice_properties",
                    new File(url.toURI()).getAbsolutePath());
            SaveService.loadProperties();
        } catch (Exception e) {
            throw new RuntimeException("加载 saveservice.properties 失败", e);
        }
    }

    /**
     * 获取JMeter引擎实例
     *
     * @return JMeter引擎实例
     */
    public static StandardJMeterEngine getJmeterEngine() {
        // 初始化配置
        initJmeterProperties();
        return new StandardJMeterEngine();
    }

}
