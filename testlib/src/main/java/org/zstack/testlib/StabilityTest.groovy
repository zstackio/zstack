package org.zstack.testlib

/**
 * Created by lining on 2017/7/12.
 */
abstract class StabilityTest extends Test implements Case{

    private static final String targetSubCaseParamKey = "cases"
    private static final String subCaseExecutionTimesKey = "times"

    private List<Case> targetSubCaseList

    SpringSpec getDefaultSpringSpec(){
        throw StopTestSuiteException()
    }

    @Override
    void setup() {
        String targetCaseList = System.getProperty(targetSubCaseParamKey)
        List<Case> caseList = buildTargetCaseList(targetCaseList)

        assert !checkSubCaseInclusionItself(caseList)

        if(caseList.size() == 1){
            Case subCase = caseList[0]
            subCase.setup()
            SpringSpec subCaseSpringSpec = subCase._springSpec
            useSpring(subCaseSpringSpec)
        } else {
            useSpring(getDefaultSpringSpec())
        }
    }

    @Override
    void environment() {
        return
    }

    @Override
    void run() {
        try {
            environment()
            test()

            logger.info("start cleanup for case ${this.class}")
            clean()
        } catch (Throwable t) {
            logger.warn("a sub case [${this.class}] fails, ${t.message}", t)
            throw t
        }
    }

    @Override
    void test() {
        String targetCaseList = System.getProperty(targetSubCaseParamKey)
        String times = System.getProperty(subCaseExecutionTimesKey)

        logger.info("start stability test, targetCaseList = ${targetCaseList}, times = ${times != null ? times : 1}")

        if(times == null){
            runTestWithTimes(targetCaseList)
        }else{
            runTestWithTimes(targetCaseList, Integer.parseInt(times))
        }

        logger.info("stability test finished")
    }

    private void runTestWithTimes(String targetCaseList, int times = 1){
        assert times >= 1

        List<Case> caseList = buildTargetCaseList(targetCaseList)

        for(Case subCase : caseList){
            int index = 0
            while (index < times){
                index ++

                logger.info("stability test, a sub case [${subCase.class}] start running, current execution times is ${index}")
                long startTime = new Date().getTime()
                try{
                    subCase.run()
                }catch (Throwable t){
                    logger.error("stability test fails, a sub case [${subCase.class}] fails, current execution times is ${index}, ${t.message}" ,t)
                    throw t
                }
                long spendTime = (new Date().getTime() - startTime) / 1000
                logger.info("stability test, a sub case [${subCase.class}] test pass, current execution times is ${index}, spend time is ${spendTime} secs")


                assert index > 0 && index <= times
            }
        }
    }

    private List<Case> buildTargetCaseList(String targetCaseList){
        if(this.targetSubCaseList != null){
            return this.targetSubCaseList
        }

        List<Case> result = []

        if(targetCaseList == null || targetCaseList.isEmpty()){
            return result
        }

        try {
            String[] caseClassNameList = targetCaseList.split(",")
            for(String caseClassName : caseClassNameList){
                Case subCase = Class.forName(caseClassName).newInstance() as Case
                subCase.metaClass.collectErrorLog = {
                    return
                }
                result.add(subCase)
            }
        }catch (Exception e){
            logger.error("build target subCase error occurred", e)
            throw e
        }
        this.targetSubCaseList = result

        String caseNames = result.collect {it.class.name}.join(", ")
        logger.debug("stability test target subcase list : ${caseNames}")

        return result
    }

    private boolean checkSubCaseInclusionItself(List<Case> cases){
        boolean result = false

        cases.each {
            if(it.class.name.equals(this.class.name)){
                result = true
            }
        }

        return result
    }

    @Override
    void clean() {
        return
    }
}
