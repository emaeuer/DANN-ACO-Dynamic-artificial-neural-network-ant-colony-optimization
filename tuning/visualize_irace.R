library("irace")

fileName <- "C:/Users/emaeu/OneDrive/Dokumente_Eric/Uni/Masterarbeit/rdata/neat_cart_pole_complete.Rdata"
load(fileName)
results <- iraceResults$testing$experiments

########################################
#retrieve the best configurations
########################################

getFinalElites(logFile = fileName, n = 0)

########################################
# show parameter frequencies
########################################

parameterFrequency(iraceResults$allConfigurations, iraceResults$parameters)

########################################
#Show parallel coordinates
########################################

# Get last iteration number
last <- length(iraceResults$iterationElites)
# Get configurations in the last two iterations
# conf <- getConfigurationByIteration(iraceResults = iraceResults,
# iterations = c(last - 1, last))
# parallelCoordinatesPlot (conf, iraceResults$parameters,
# param_names = c("betaC", "gammaC", "eta", "zeta", "delta", "epsilon", "theta"),
# hierarchy = FALSE)

########################################
#Show box plots
########################################

configurationsBoxplot(results, ylab = "Solution cost")

########################################
# Show mean value series
########################################

#Get number of iterations
iters <- unique(iraceResults$experimentLog[, "iteration"])
# Get number of experiments (runs of target-runner) up to each iteration
fes <- cumsum(table(iraceResults$experimentLog[,"iteration"]))
# Get the mean value of all experiments executed up to each iteration
# for the best configuration of that iteration.
elites <- as.character(iraceResults$iterationElites)
values <- colMeans(iraceResults$testing$experiments[, elites])
plot(fes, values, type = "s",
     xlab = "Number of runs of the target algorithm",
     ylab = "Mean value over testing set")
points(fes, values)
text(fes, values, elites, pos = 1)