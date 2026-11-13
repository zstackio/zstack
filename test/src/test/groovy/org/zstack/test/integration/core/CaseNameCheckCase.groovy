package org.zstack.test.integration.core

import org.zstack.core.Platform
import org.reflections.Reflections
import org.zstack.test.integration.stabilisation.StabilityTestCase
import org.zstack.testlib.SubCase
import org.zstack.testlib.Test
import org.zstack.testlib.Case
import org.zstack.testlib.collectstrategy.NearestSubCaseCollectionStrategy
import org.zstack.testlib.collectstrategy.MostCompleteSubCaseCollectionStrategy

import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

import java.lang.reflect.Modifier

/**
 * Created by lining on 2017/7/21.
 */
class CaseNameCheckCase extends SubCase {

    private static final List<String> ignoreCheckListForTestSuite = [StabilityTestCase.class.name]

    @Override
    void clean() {
    }

    @Override
    void setup() {
    }

    @Override
    void environment() {
    }

    @Override
    void test() {
        checkTestSuiteName()
        checkTestCaseName()
        checkSubCasePackageBoundary()
    }

    void checkSubCasePackageBoundary() {
        def loader = new GroovyClassLoader(getClass().classLoader)
        def fixture = { String packageName, String name, boolean suite ->
            loader.parseClass("""package ${packageName}
                class ${name} extends org.zstack.testlib.${suite ? 'Test' : 'SubCase'} {
                    void setup() {}
                    void environment() {}
                    void test() {}
                    void clean() {}
                }""")
        }
        def suite = fixture('collectorfixture.storage', 'StorageTest', true)
        def childSuite = fixture('collectorfixture.storage.child', 'ChildTest', true)
        def siblingSuite = fixture('collectorfixture.storageDevice', 'StorageDeviceTest', true)
        def rootCase = fixture('collectorfixture.storage', 'RootCase', false)
        def localCase = fixture('collectorfixture.storage.local', 'LocalCase', false)
        def childCase = fixture('collectorfixture.storage.child', 'ChildCase', false)
        def deepCase = fixture('collectorfixture.storage.child.deep', 'DeepCase', false)
        def siblingCase = fixture('collectorfixture.storageDevice', 'DeviceCase', false)
        def orphanSibling = fixture('collectorfixture.storageEncryptGateway', 'GatewayCase', false)
        def reflections = mock(Reflections.class)
        when(reflections.getSubTypesOf(Test.class)).thenReturn([suite, childSuite, siblingSuite] as Set)
        when(reflections.getSubTypesOf(Case.class)).thenReturn(
                [rootCase, localCase, childCase, deepCase, siblingCase, orphanSibling] as Set)

        def originalReflections = Platform.reflections
        def field = Platform.class.getDeclaredField('reflections')
        field.accessible = true
        try {
            field.set(null, reflections)
            def nearest = new NearestSubCaseCollectionStrategy()
            assert nearest.collectSubCases(suite.newInstance()).toSet() == [rootCase, localCase] as Set
            assert nearest.collectSubCases(childSuite.newInstance()).toSet() == [childCase, deepCase] as Set
            assert nearest.collectSubCases(siblingSuite.newInstance()).toSet() == [siblingCase] as Set
            assert new MostCompleteSubCaseCollectionStrategy().collectSubCases(suite.newInstance()).toSet() ==
                    [rootCase, localCase, childCase, deepCase] as Set
        } finally {
            field.set(null, originalReflections)
            loader.close()
        }
    }

    void checkTestSuiteName(){
        List<String> invalidNameList = []

        def tests = Platform.reflections.getSubTypesOf(Test.class).findAll { !Modifier.isAbstract(it.modifiers) }
        tests.forEach{ it ->
            Class caseClass = it
            boolean result = it.package != null && !it.name.endsWith("Test") && !SubCase.isAssignableFrom(it) && ignoreCheckListForTestSuite.find{ caseClass.name == it } == null

            if(result){
                invalidNameList.add(it.name)
            }
        }

        assert 0 == invalidNameList.size() : "invalid testsuite name, testsuite name must end with 'Test', ${invalidNameList}"
    }

    void checkTestCaseName(){
        List<String> invalidNameList = []

        def tests = Platform.reflections.getSubTypesOf(SubCase.class).findAll { !Modifier.isAbstract(it.modifiers) }
        tests.forEach{ it ->
            boolean result = it.package != null && !it.name.endsWith("Case")

            if(result){
                invalidNameList.add(it.name)
            }
        }

        assert 0 == invalidNameList.size() : "invalid SubCase name, testsuite name must end with 'Case', ${invalidNameList}"
    }

}
