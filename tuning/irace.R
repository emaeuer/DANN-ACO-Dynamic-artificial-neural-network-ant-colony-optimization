# uncomment for installation
#install.packages("irace")


library("irace")

#Test installation
#system.file(package = "irace")

scenario <- readScenario(filename = "tuning/scenario.txt",
                         scenario = defaultScenario())

parameters <- readParameters(file = "tuning/parameters.txt")

# check scenario
#checkIraceScenario(scenario = scenario)

# run scenario
irace.main(scenario = scenario)
