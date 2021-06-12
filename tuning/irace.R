library("irace")

scenario <- readScenario(filename = "tuning/scenario.txt",
                         scenario = defaultScenario())

irace.main(scenario = scenario)
