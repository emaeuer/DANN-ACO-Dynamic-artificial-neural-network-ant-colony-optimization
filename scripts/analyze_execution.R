library(ggplot2)
library(hrbrthemes)
library(viridis)
library(dplyr)

setClass("Configuration", slots=list(runNumber="numeric", maxFitness="numeric", name="character"))

extractIterationData <- function(filepath) {
  df <- data.frame()
  con <- file(filepath, "r")

  currentRow <- data.frame(runID = 0, evaluation = 0)

  configuration <- new("Configuration")

  while (TRUE) {
    line <- readLines(con, n = 1)
    if (length(line) == 0) {
      break
    }

    if (grepl("^OPTIMIZATION_CONFIGURATION\\.METHOD_NAME", line)) {
      configuration@name <- str_split(line, "=", 2)[[1]][2]
    } else if (grepl("^ENVIRONMENT_CONFIGURATION\\.MAX_FITNESS_SCORE", line)) {
      configuration@maxFitness <- as.numeric(str_split(line, "=", 2)[[1]][2])
    } else if (grepl("^OPTIMIZATION_CONFIGURATION\\.NUMBER_OF_RUNS", line)) {
      configuration@runNumber <- as.numeric(str_split(line, "=", 2)[[1]][2])
    } else if (grepl("^GENERAL_STATE\\.RUN_\\d*?\\.RUN_NUMBER", line)) {
      if (currentRow$runID != 0) {
        df <- rbind(df, currentRow)
      }
      currentRow$runID <- as.numeric(str_split(line, " = ", 2)[[1]][2])
    } else if (grepl("^GENERAL_STATE\\.RUN_\\d*?\\.EVALUATION_NUMBER", line)) {
      currentEvaluation <- as.numeric(str_split(line, " = ", 2)[[1]][2])
      if (currentRow$evaluation < currentEvaluation && currentRow$evaluation != 0) {
        df <- rbind(df, currentRow)
      }
      currentRow$evaluation <- currentEvaluation
    } else if (grepl("^GENERAL_STATE\\.RUN_\\d*?\\.FITNESS_VALUES", line)) {
      csvValue <- str_split(line, " = ", 2)[[1]][2]
      values <- as.numeric(str_split(csvValue, ",")[[1]])
      currentRow$fitnessValues <- list(values)
    } else if (grepl("^GENERAL_STATE\\.RUN_\\d*?\\.USED_HIDDEN_NODES", line)) {
      csvValue <- str_split(line, " = ", 2)[[1]][2]
      values <- as.numeric(str_split(csvValue, ",")[[1]])
      currentRow$hiddenNodes <- list(values)
    } else if (grepl("^GENERAL_STATE\\.RUN_\\d*?\\.USED_CONNECTIONS", line)) {
      csvValue <- str_split(line, " = ", 2)[[1]][2]
      values <- as.numeric(str_split(csvValue, ",")[[1]])
      currentRow$connections <- list(values)
    }
  }

  close(con)
  return(list("data" = df, "configuration" = configuration))
}

drawRunSummary <- function(data, runNumber) {
  # can easily be modified to show hidden nodes or connection number

  filteredData <- data %>%
    filter(runID == runNumber) %>%
    rowwise() %>%
    mutate(maxFitness = max(unlist(fitnessValues)), avgFitness = mean(unlist(fitnessValues))) %>%
    select(runID, evaluation, maxFitness, avgFitness) %>%
    gather(key = key, val = value, maxFitness, avgFitness)

  ggplot(filteredData, aes(x = evaluation, value, fill = evaluation)) +
    geom_line(size = 1, aes(colour = paste(runID, key), group = paste(runID, key))) +
    theme(axis.title.x=element_text(size = 18, family = "LM Roman 10"),
          axis.title.y=element_text(size = 18, family = "LM Roman 10"),
          panel.border = element_rect(colour = "black", fill=NA, size=1),
          plot.title = element_text(family = "LM Roman 10", hjust = 0.5, size=24, margin=margin(0,0,15,0)),
          axis.text = element_text(size = 18, family = "LM Roman 10"))
}

pacoResult <- extractIterationData("C:\\Users\\emaeu\\IdeaProjects\\ParticleEnvironment\\temp\\execution_2021-07-01_14-08-56-365.txt")
pacoIterationData <- pacoResult[["data"]]
pacoConfiguration <- pacoResult[["configuration"]]

pacoFilteredData <- pacoIterationData %>%
  rowwise() %>%
  mutate(fitness = max(unlist(fitnessValues)), type = pacoConfiguration@name) %>%
  select(runID, evaluation, fitness, type) %>%
  group_by(evaluation) %>%
  summarise(fitness = (sum(fitness) + (pacoConfiguration@runNumber - n()) * pacoConfiguration@maxFitness) / pacoConfiguration@runNumber, type = type)

neatResult <- extractIterationData("C:\\Users\\emaeu\\IdeaProjects\\ParticleEnvironment\\temp\\execution_2021-07-02_10-24-15-193.txt")
neatIterationData <- neatResult[["data"]]
neatConfiguration <- neatResult[["configuration"]]

neatFilteredData <- neatIterationData %>%
  rowwise() %>%
  mutate(fitness = max(unlist(fitnessValues)), type = neatConfiguration@name) %>%
  select(runID, evaluation, fitness, type) %>%
  group_by(evaluation) %>%
  summarise(fitness = (sum(fitness) + (neatConfiguration@runNumber - n()) * neatConfiguration@maxFitness) / neatConfiguration@runNumber, type = type)

ggplot(rbind(pacoFilteredData, neatFilteredData), aes(x = evaluation, y = fitness, color=type, group=type)) +
  geom_line(size = 1) +
  theme(axis.title.x=element_text(size = 18, family = "LM Roman 10"),
        axis.title.y=element_text(size = 18, family = "LM Roman 10"),
        panel.border = element_rect(colour = "black", fill=NA, size=1),
        plot.title = element_text(family = "LM Roman 10", hjust = 0.5, size=24, margin=margin(0,0,15,0)),
        axis.text = element_text(size = 18, family = "LM Roman 10"))



