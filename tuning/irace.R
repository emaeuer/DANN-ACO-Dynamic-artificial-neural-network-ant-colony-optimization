# uncomment for installation
#install.packages("irace")

library("irace")

#Test installation
#system.file(package = "irace")

optimizationMethod <- "neat"

if (optimizationMethod == "neat") {
  scenario <- readScenario(filename = "tuning/neat/scenario.txt",
                           scenario = defaultScenario())

  parameters <- readParameters(file = "tuning/neat/parameters.txt")
} else if (optimizationMethod == "aco") {
  scenario <- readScenario(filename = "tuning/aco/scenario.txt",
                           scenario = defaultScenario())

  parameters <- readParameters(file = "tuning/aco/parameters.txt")
}

# check scenario
# checkIraceScenario(scenario = scenario)

# run scenario
irace.main(scenario = scenario)
