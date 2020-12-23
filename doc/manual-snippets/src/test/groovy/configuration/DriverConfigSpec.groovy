/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package configuration

import geb.driver.CachingDriverFactory
import geb.test.WebDriverServer
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.openqa.selenium.firefox.FirefoxDriver
import org.openqa.selenium.htmlunit.HtmlUnitDriver
import org.openqa.selenium.remote.RemoteWebDriver
import spock.lang.Specification
import spock.lang.Unroll

import static geb.properties.SystemPropertiesUtil.loadSystemPropertiesFromFile

class DriverConfigSpec extends Specification implements InlineConfigurationLoader {

    @Rule
    TemporaryFolder temporaryFolder

    def setupSpec() {
        CachingDriverFactory.clearCacheAndQuitDriver()

        loadSystemPropertiesFromFile("build/webdriver-binaries/webdriver-binaries.properties")
    }

    def "configuring driver using closure"() {
        when:
        configScript """
            // tag::configuring_driver[]
            import org.openqa.selenium.firefox.FirefoxDriver

            driver = { new FirefoxDriver() }
            // end::configuring_driver[]
        """

        then:
        config.driver instanceof FirefoxDriver

        cleanup:
        CachingDriverFactory.clearCacheAndQuitDriver()
    }

    def "configuring driver using class name"() {
        when:
        configScript """
            // tag::configuring_driver_using_class_name[]
            driver = "org.openqa.selenium.firefox.FirefoxDriver"
            // end::configuring_driver_using_class_name[]
        """

        then:
        config.driver instanceof FirefoxDriver

        cleanup:
        CachingDriverFactory.clearCacheAndQuitDriver()
    }

    def "configuring driver using driver name"() {
        when:
        configScript """
            // tag::configuring_driver_using_driver_name[]
            driver = "firefox"
            // end::configuring_driver_using_driver_name[]
        """

        then:
        config.driver instanceof FirefoxDriver

        cleanup:
        CachingDriverFactory.clearCacheAndQuitDriver()
    }

    @Unroll("driver should be #driverClass.simpleName when environment is #env")
    def "environment sensitive driver config"() {
        given:
        def webDriverServer = new WebDriverServer()
        webDriverServer.start()

        when:
        configScript(env, """
            // tag::env_sensitive_driver_config[]
            import org.openqa.selenium.htmlunit.HtmlUnitDriver

            import org.openqa.selenium.remote.DesiredCapabilities
            import org.openqa.selenium.remote.RemoteWebDriver

            // default is to use htmlunit
            driver = { new HtmlUnitDriver() }

            environments {
                // when system property 'geb.env' is set to 'remote' use a remote Firefox driver
                remote {
                    driver = {
                        def remoteWebDriverServerUrl = new URL("http://example.com/webdriverserver")
                        // end::env_sensitive_driver_config[]
                        remoteWebDriverServerUrl = new URL("${webDriverServer.getBaseUrl()}")
                        // tag::env_sensitive_driver_config[]
                        new RemoteWebDriver(remoteWebDriverServerUrl, DesiredCapabilities.firefox())
                    }
                }
            }

            // end::env_sensitive_driver_config[]
        """)

        then:
        config.driver in driverClass

        cleanup:
        CachingDriverFactory.clearCacheAndQuitDriver()
        webDriverServer.stop()

        where:
        env      | driverClass
        null     | HtmlUnitDriver
        "remote" | RemoteWebDriver
    }
}