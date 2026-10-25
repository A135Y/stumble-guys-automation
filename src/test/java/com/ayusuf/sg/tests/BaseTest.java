package com.ayusuf.sg.tests;

import com.ayusuf.sg.config.Config;
import com.ayusuf.sg.driver.DriverManager;
import com.ayusuf.sg.support.Artifacts;
import com.ayusuf.sg.support.ScreenRecorder;
import com.ayusuf.sg.support.StepTimer;
import org.openqa.selenium.WebDriver;
import org.testng.ITestResult;
import org.testng.SkipException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public abstract class BaseTest {

    protected WebDriver driver;
    private ScreenRecorder recorder;

    @BeforeMethod(alwaysRun = true)
    public void startDriver(Method method) {
        skipIfWrongPlatform(method);
        StepTimer.start();
        driver = DriverManager.start();
        StepTimer.step("browser started");
        if (Config.getBool("record")) recorder = ScreenRecorder.start(driver);
    }

    private static void skipIfWrongPlatform(Method method) {
        boolean mobile = Config.platform().isMobile();
        DesktopOnly desktopOnly = annotation(method, DesktopOnly.class);
        if (mobile && desktopOnly != null) throw new SkipException("Desktop only: " + desktopOnly.value());
        MobileOnly mobileOnly = annotation(method, MobileOnly.class);
        if (!mobile && mobileOnly != null) throw new SkipException("Mobile only: " + mobileOnly.value());
    }

    private static <A extends Annotation> A annotation(Method method, Class<A> type) {
        A onMethod = method.getAnnotation(type);
        return onMethod != null ? onMethod : method.getDeclaringClass().getAnnotation(type);
    }

    @AfterMethod(alwaysRun = true)
    public void stopDriver(ITestResult result) {
        if (driver == null) return; // skipped before the browser started
        StepTimer.step("test body done");
        if (recorder != null) {
            recorder.stop(result.getTestClass().getRealClass().getSimpleName() + "." + result.getMethod().getMethodName())
                    .ifPresent(video -> System.out.println("Video: " + video.toAbsolutePath()));
            recorder = null;
        }
        try {
            if (result.getStatus() == ITestResult.FAILURE) {
                String name = result.getMethod().getMethodName();
                System.out.println("Screenshot: " + Artifacts.screenshot(driver, name).toAbsolutePath());
            }
        } catch (RuntimeException e) {
            System.err.println("Could not save screenshot: " + e.getMessage());
        } finally {
            DriverManager.quit();
            driver = null;
            StepTimer.step("browser closed");
        }
    }
}
