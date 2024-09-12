package com.mycompany.app;

import org.openqa.selenium.By;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.chromium.ChromiumDriverLogLevel;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;

public class Main {
    public static void main(String[] args) throws IOException {
        // Set the proxy details
        String proxyAddress = "127.0.0.1";
        int proxyPort = 7890;
        String site = "https://us.shein.com/";
        String searchContent = "Jeans";

        WebDriver driver = getWebDriver(proxyAddress, proxyPort);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(2)); // 设置隐式等待时长

        // 访问首页
        driver.get(site);
        String title = driver.getTitle();
        System.out.println(title);

        Wait<WebDriver> wait = new WebDriverWait(driver, Duration.ofSeconds(10)); // 显式等待时长

        // 查找并点击搜索框
        driver.findElement(By.className("header-search-input")).sendKeys(searchContent);
        while (true) {
            try {
                // 然后点击搜索按钮
                // 页面有广告弹窗，会导致搜索按钮无法点击
                //wait.until(ExpectedConditions.presenceOfElementLocated(By.className("header-search-input")));
                driver.findElement(By.className("search-btn")).click();
                break;
            } catch (Exception e) {
                try {
                    // 尝试关闭广告弹窗
                    wait.until(ExpectedConditions.presenceOfElementLocated(By.className("dialog-header-v2__close-btn")));
                    driver.findElement(By.className("dialog-header-v2__close-btn")).findElement(By.tagName("svg")).click();
                } catch (Exception ignore) {
                }
            }
        }

        // 读取商品列表
        Set<String> record = new HashSet<>();
        StringBuilder sb = new StringBuilder("标题,价格,图片\n");
        for (int i = 0; i < 3; i++) {
            wait.until(ExpectedConditions.presenceOfElementLocated(By.className("product-card")));
            List<WebElement> productCards = driver.findElements(By.className("product-card"));
            System.out.println("第一页找到" + productCards.size() + "个商品");
            productCards.forEach((productCard) -> {
                WebElement nameLink = productCard.findElement(By.className("goods-title-link"));
                String goodsTitle = nameLink.getText().replace(",", "_");
                if (!record.contains(goodsTitle)) {
                    WebElement priceSpan = productCard.findElement(By.className("product-item__camecase-wrap"));
                    WebElement imgTag = productCard.findElement(By.tagName("img"));
                    String goodsInOneLine = goodsTitle + "," + priceSpan.getText() + "," + imgTag.getDomProperty("src");
                    System.out.println(goodsInOneLine);
                    sb.append(goodsInOneLine).append("\n");
                    record.add(goodsTitle);
                }
            });
        }

        // 备份页面到文件
        File dir = new File("out");
        if (!dir.exists()) {
            Files.createDirectory(dir.toPath());
        }
        String d = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
        FileWriter writer = new FileWriter("out/page_" + d + ".html");
        writer.write(Objects.requireNonNull(driver.getPageSource()));
        writer.close();

        FileWriter writer2 = new FileWriter("out/page_goods_" + d + ".csv");
        writer2.write(sb.toString());
        writer2.close();

        driver.quit(); // 完成后退出
    }

    private static WebDriver getWebDriver(String proxyAddress, int proxyPort) {
        ChromeOptions options = getChromeOptions(proxyAddress, proxyPort);

        // 控制台输出日志
        ChromeDriverService service = new ChromeDriverService.Builder()
                .withLogOutput(System.out)
                .withLogLevel(ChromiumDriverLogLevel.INFO)
                .build();
        return new ChromeDriver(service, options);
    }

    private static ChromeOptions getChromeOptions(String proxyAddress, int proxyPort) {
        ChromeOptions options = new ChromeOptions();

        // chrome.exe 支持的启动参数：https://github.com/GoogleChrome/chrome-launcher/blob/main/docs/chrome-flags-for-tools.md
        // chrome driver 支持的参数：https://developer.chrome.com/docs/chromedriver/capabilities

        // 禁止浏览器提示受自动化控制
        options.setExperimentalOption("useAutomationExtension", false);

        // 设置代理
        if (!proxyAddress.isEmpty()) {
            Proxy proxy = new Proxy();
            proxy.setHttpProxy(proxyAddress + ":" + proxyPort);
            proxy.setSslProxy(proxyAddress + ":" + proxyPort);
            // proxy.setSocksProxy(proxyAddress+":"+proxyPort);
            // proxy.setSocksVersion(5);
            options.setProxy(proxy);
        }

        // 添加 UA
        options.addArguments("user-agent=\"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36\"");

        // 禁止运行js
//        Map<String, Object> chromePrefs = new HashMap<>(2);
//        chromePrefs.put("profile.managed_default_content_settings.javascript", 2);
//        options.setExperimentalOption("prefs", chromePrefs);

        // 开启 headless 模式，即无界面方式运行
        //options.addArguments("--headless");
        return options;
    }
}
