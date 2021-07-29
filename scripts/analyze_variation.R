library(ggplot2)
library(hrbrthemes)
library(viridis)
library(dplyr)
library(tidyr)
library(stringr)

extractConfiguration <- function (configuration_str) {
  map <- list()

  configuration_components <- str_split(configuration_str, "\\]\\[")[[1]]
  configuration_components <- gsub("\\]|\\[|\\s*", "", configuration_components)

  for (component in configuration_components) {
    component_parts <- str_split(component, "=")[[1]]
    name <- component_parts[1]
    value <- as.numeric(component_parts[2])

    map[[name]] <- value


  }

  return(map)
}

extractNumber <- function (number_str) {
  return(as.numeric(gsub("\\,", ".", number_str)))
}

extractModifications <- function (modification_str) {
  map <- list()

  # ADD:5516 (0,03), REMOVE:6477 (0,03), NOTHING:92605 (0,48), SPLIT:89875 (0,46)

  modficaition_components <- str_split(modification_str, ", ")[[1]]
  modficaition_components <- gsub(":\\d* \\(", "=", modficaition_components)
  modficaition_components <- gsub("\\)", "", modficaition_components)
  modficaition_components <- gsub("\\,", ".", modficaition_components)

  for (component in modficaition_components) {
    component_parts <- str_split(component, "=")[[1]]
    name <- component_parts[1]
    value <- as.numeric(component_parts[2])

    map[[name]] <- value
  }

  return(map)
}

parseTableToDataFrame <- function(filepath) {
  result <- data.frame()

  filepath <- "C:/Users/emaeu/Desktop/connection_variation_result.txt"
  file <- file(filepath, "r")

  while (TRUE) {
    row <- list()

    line <- readLines(file, n = 1, encoding = "UTF-8")
    if (length(line) == 0) {
      break
    }
    row_data <- str_split(line, "\\s*│\\s*")[[1]]
    row <- c(row, extractConfiguration(row_data[2]))
    row <- c(row, evaluations = extractNumber(row_data[3]))
    row <- c(row, neurons = extractNumber(row_data[5]))
    row <- c(row, connections = extractNumber(row_data[6]))
    row <- c(row, successRate = extractNumber(row_data[7]))
    row <- c(row, extractModifications(row_data[8]))

    result <- rbind(result, row)
  }

  close(file)
  return(result)
}

data <- parseTableToDataFrame(filepath <- "C:/Users/emaeu/Desktop/variation_result.txt")

filteredData <- data %>%
  rowwise() %>%
  filter(is.finite(evaluations))

ggplot(filteredData, aes(x = alphaC, y = gammaC, fill = connections)) +
  geom_tile() +
  scale_fill_viridis(discrete=FALSE, direction = -1) +
  theme(axis.title.x=element_text(size = 18, family = "LM Roman 10"),
        axis.title.y=element_text(size = 18, family = "LM Roman 10"),
        panel.border = element_rect(colour = "black", fill=NA, size=1),
        plot.title = element_text(family = "LM Roman 10", hjust = 0.5, size=24, margin=margin(0,0,15,0)),
        axis.text = element_text(size = 18, family = "LM Roman 10"))