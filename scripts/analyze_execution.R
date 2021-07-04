library(ggplot2)
library(hrbrthemes)
library(viridis)
library(dplyr)
library(tidyr)

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
    } else if (grepl("^GENERAL_STATE\\.RUN_\\d*?\\.PACO\\.CONNECTION_WEIGHTS_SCATTERED", line)) {
      value <- str_split(line, " = ", 2)[[1]][2]
      singleLists <- str_split(value, "\\]\\,\\[")
      singleLists <- str_replace_all(unlist(singleLists), "\\[|\\]*", "")
      resultList <- list()
      for (l in singleLists) {
        l <- unlist(str_split(l, ":", 2))
        resultList[l[1]] <- list(as.numeric(unlist(str_split(l[2], ", "))))
      }
      currentRow$weightDistribution <- list(resultList)
    }
  }

  close(con)
  return(list("data" = df, "configuration" = configuration))
}

drawRunSummary <- function(fileName, runNumber) {
  # can easily be modified to show hidden nodes or connection number

  result <- extractIterationData(fileName)
  data <- result[["data"]]
  configuration <- result[["configuration"]]

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

compareResultsFromDifferentFiles <- function(...) {
  df <- data.frame()

  for (fileName in list(...)) {
    result <- extractIterationData(fileName)
    data <- result[["data"]]
    configuration <- result[["configuration"]]

    filteredData <- data %>%
      rowwise() %>%
      mutate(fitness = max(unlist(fitnessValues)), type = configuration@name) %>%
      select(runID, evaluation, fitness, type) %>%
      group_by(evaluation) %>%
      summarise(fitness = (sum(fitness) + (configuration@runNumber - n()) * configuration@maxFitness) / configuration@runNumber, type = type)

    df <- rbind(df, filteredData)
  }

  ggplot(df, aes(x = evaluation, y = fitness, color=type, group=type)) +
    geom_line(size = 1) +
    theme(axis.title.x=element_text(size = 18, family = "LM Roman 10"),
          axis.title.y=element_text(size = 18, family = "LM Roman 10"),
          panel.border = element_rect(colour = "black", fill=NA, size=1),
          plot.title = element_text(family = "LM Roman 10", hjust = 0.5, size=24, margin=margin(0,0,15,0)),
          axis.text = element_text(size = 18, family = "LM Roman 10"))
}

compareResultsFromDifferentFiles <- function(...) {
  df <- data.frame()

  for (fileName in list(...)) {
    result <- extractIterationData(fileName)
    data <- result[["data"]]
    configuration <- result[["configuration"]]

    filteredData <- data %>%
      rowwise() %>%
      mutate(fitness = max(unlist(fitnessValues)), type = configuration@name) %>%
      select(runID, evaluation, fitness, type) %>%
      group_by(evaluation) %>%
      summarise(fitness = (sum(fitness) + (configuration@runNumber - n()) * configuration@maxFitness) / configuration@runNumber, type = type)

    df <- rbind(df, filteredData)
  }

  ggplot(df, aes(x = evaluation, y = fitness, color=type, group=type)) +
    geom_line(size = 1) +
    theme(axis.title.x=element_text(size = 18, family = "LM Roman 10"),
          axis.title.y=element_text(size = 18, family = "LM Roman 10"),
          panel.border = element_rect(colour = "black", fill=NA, size=1),
          plot.title = element_text(family = "LM Roman 10", hjust = 0.5, size=24, margin=margin(0,0,15,0)),
          axis.text = element_text(size = 18, family = "LM Roman 10"))
}

boxPlotEvaluations <- function(...) {
  df <- data.frame()

  for (fileName in list(...)) {
    result <- extractIterationData(fileName)
    data <- result[["data"]]
    configuration <- result[["configuration"]]

    filteredData <- data %>%
      rowwise() %>%
      group_by(runID) %>%
      summarise(fitness = max(evaluation), hiddenNodes = max(unlist(hiddenNodes)), connections = max(unlist(connections)), type = configuration@name)

    df <- rbind(df, filteredData)
  }

  ggplot(df, aes(x = type, y = connections)) +
    geom_boxplot() +
    theme(axis.title.x=element_text(size = 18, family = "LM Roman 10"),
          axis.title.y=element_text(size = 18, family = "LM Roman 10"),
          panel.border = element_rect(colour = "black", fill=NA, size=1),
          plot.title = element_text(family = "LM Roman 10", hjust = 0.5, size=24, margin=margin(0,0,15,0)),
          axis.text = element_text(size = 18, family = "LM Roman 10"))
}

trendOfWeights <- function(...) {
  result <- extractIterationData("C:\\Users\\emaeu\\IdeaProjects\\ParticleEnvironment\\temp\\execution_2021-07-03_20-55-46-238.txt")

  data <- result[["data"]]
  weights <- data$weightDistribution[2]
  weights[[1]][["1"]]

  filteredData <- data %>%
    rowwise() %>%
    select(evaluation, weightDistribution) %>%
    mutate(connectionID = list(names(weightDistribution))) %>%
    unnest(connectionID) %>%
    rowwise() %>%
    mutate(weights = list(weightDistribution[[connectionID]])) %>%
    mutate(mean = mean(weights), std = if_else(length(weights) == 1, 0, sd(weights))) %>%
    select(evaluation, connectionID, mean, std) %>%
    filter(connectionID %in% c (0, 1))

  ggplot(filteredData, aes(x = evaluation, y = mean, ymin = mean - std, ymax = mean + std, group = connectionID, color = connectionID, fill = connectionID)) +
    geom_line() +
    geom_ribbon(alpha=0.2) +
    theme(axis.title.x=element_text(size = 18, family = "LM Roman 10"),
          axis.title.y=element_text(size = 18, family = "LM Roman 10"),
          panel.border = element_rect(colour = "black", fill=NA, size=1),
          plot.title = element_text(family = "LM Roman 10", hjust = 0.5, size=24, margin=margin(0,0,15,0)),
          axis.text = element_text(size = 18, family = "LM Roman 10"))
}

# drawRunSummary("C:\\Users\\emaeu\\IdeaProjects\\ParticleEnvironment\\temp\\execution_2021-07-01_14-08-56-365.txt", 5)

# compareResultsFromDifferentFiles("C:\\Users\\emaeu\\IdeaProjects\\ParticleEnvironment\\temp\\execution_2021-07-01_14-08-56-365.txt",
#                                  "C:\\Users\\emaeu\\IdeaProjects\\ParticleEnvironment\\temp\\execution_2021-07-02_10-24-15-193.txt")

# boxPlotEvaluations("C:\\Users\\emaeu\\IdeaProjects\\ParticleEnvironment\\temp\\execution_2021-07-01_14-08-56-365.txt",
#                                  "C:\\Users\\emaeu\\IdeaProjects\\ParticleEnvironment\\temp\\execution_2021-07-02_10-24-15-193.txt")

trendOfWeights("C:\\Users\\emaeu\\IdeaProjects\\ParticleEnvironment\\temp\\execution_2021-07-01_14-08-56-365.txt")



