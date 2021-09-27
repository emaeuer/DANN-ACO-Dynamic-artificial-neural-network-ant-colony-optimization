library(ggplot2)
library(ggpubr)
library(hrbrthemes)
library(viridis)
library(dplyr)
library(tidyr)
library(stringr)
library(Rcpp)
library(Rfast)

extractConfiguration <- function (configuration_str) {
  map <- list()

  configuration_components <- str_split(configuration_str, "\\]\\[")[[1]]
  configuration_components <- gsub("\\]|\\[|\\s*", "", configuration_components)

  for (component in configuration_components) {
    component_parts <- str_split(component, "=")[[1]]
    name <- component_parts[1]
    value <- component_parts[2]

    if (!is.na(as.numeric(value))) {
      value <- as.numeric(value)
    }

    map[[name]] <- value
  }

  return(map)
}

extractNumber <- function (number_str) {
  return(as.numeric(gsub("\\,", ".", number_str)))
}

extractModifications <- function (modification_str) {
  map <- list(ADD = 0, REMOVE = 0, NOTHING = 0, SPLIT = 0)

  modficaition_components <- str_split(modification_str, ", ")[[1]]
  modficaition_components <- gsub(":", "=", modficaition_components)
  modficaition_components <- gsub("\\s\\(.*\\)", "", modficaition_components)

  for (component in modficaition_components) {
    component_parts <- str_split(component, "=")[[1]]
    name <- component_parts[1]
    value <- as.numeric(component_parts[2])

    if (name != "") {
      map[[name]] <- value
    }
  }

  return(map)
}

parseTableToDataFrame <- function(filepath) {
  result <- data.frame()
  file <- file(filepath, "r")

  while (TRUE) {
    row <- list()

    line <- readLines(file, n = 1, encoding = "UTF-8")
    if (length(line) == 0) {
      break
    }
    row_data <- str_split(line, "\\s*│\\s*")[[1]]
    row <- c(row, extractConfiguration(row_data[2]))
    row <- c(row, problem = row_data[3])
    row <- c(row, evaluations = extractNumber(row_data[4]))
    row <- c(row, time = extractNumber(row_data[5]))
    row <- c(row, neurons = extractNumber(row_data[6]))
    row <- c(row, connections = extractNumber(row_data[7]))
    row <- c(row, successRate = extractNumber(row_data[8]))
    row <- c(row, extractModifications(row_data[9]))
    row <- c(row, allEvaluations = row_data[10])
    row <- c(row, deviation = extractNumber(row_data[11]))

    result <- rbind(result, row)
  }

  close(file)
  return(result)
}

data <- parseTableToDataFrame("temp/variation_split_parameters.result")

filteredData <- data %>%
  rowwise() %>%
  mutate(allEvaluations = list(as.numeric(str_split(allEvaluations, ",")[[1]]))) %>%
  mutate(average = mean(allEvaluations, na.rm = TRUE)) %>%
  mutate(evals = if (is.finite(evaluations) & evaluations >= 40000) NA else average) %>%
  mutate(successRate = if (length(unlist(allEvaluations)) < 20) 1 - (length(unlist(allEvaluations)) / 25) else 1 - successRate, actions = (ADD + REMOVE + SPLIT) / (ADD + REMOVE + SPLIT + NOTHING), splits = SPLIT / (ADD + REMOVE + SPLIT + NOTHING)) %>%
  filter(problem == "XOR") %>%
  filter(t %in% c(0, 1.07, 5)) %>%
  mutate(t = paste0("θ = ", t)) %>%
  mutate(primaryAction = if (max(ADD, REMOVE, SPLIT) == 0) "" else if (ADD == Rfast::nth(c(ADD, REMOVE, SPLIT), 1, descending = TRUE)) "Verbindung einfügen" else if (REMOVE == Rfast::nth(c(ADD, REMOVE, SPLIT), 1, descending = TRUE)) "Verbindung entfernen" else if (SPLIT == Rfast::nth(c(ADD, REMOVE, SPLIT), 1, descending = TRUE)) "Verbindung teilen" else "") %>%
  mutate(secondaryAction = if (max(ADD, REMOVE, SPLIT) == 0) "" else if (ADD == Rfast::nth(c(ADD, REMOVE, SPLIT), 2, descending = TRUE)) "Verbindung einfügen" else if (REMOVE == Rfast::nth(c(ADD, REMOVE, SPLIT), 2, descending = TRUE)) "Verbindung entfernen" else if (SPLIT == Rfast::nth(c(ADD, REMOVE, SPLIT), 2, descending = TRUE)) "Verbindung teilen" else "") %>%
  mutate(splitRate = SPLIT / (SPLIT + REMOVE))

ggplot(filteredData, aes(x = z, y = e, fill = splitRate)) +
  geom_tile() +
  scale_fill_viridis(discrete=FALSE, direction = -1, na.value = "black") +
  facet_wrap(as.factor(t) ~ ., ncol = 3) +
  xlab("ζ") +
  ylab("η") +
  labs(fill = "Anteil Teilungen", size = "Fehlerrrate") +
  # ggtitle("Cart pole split rate") +
  # guides(
  #   fill = guide_legend(title = "Primäre Modifikation", order = 1, override.aes=list(shape=FALSE, colour = "transparent")),
  #   shape = guide_legend(title = "Sekundäre Modifikation", order = 2)
  # ) +
  # geom_point(data = subset(filteredData, successRate > 0), aes(size = successRate), color = "gray48") +
  # geom_point(size = 5, color = "gray48") +
  theme(axis.title.x=element_text(size = 18, family = "CMU Classical Serif"),
        axis.title.y=element_text(size = 18, family = "CMU Classical Serif"),
        panel.border = element_rect(colour = "black", fill=NA, size=1),
        plot.title = element_text(family = "LM Roman 10", hjust = 0.5, size=24),
        axis.text = element_text(size = 18, family = "LM Roman 10"),
        strip.text.x = element_text(size = 18, family = "Century"),
        strip.background = element_blank(),
        legend.title = element_text(size = 16, family = "LM Roman 10", margin=margin(0,0,5,0)),
        legend.text = element_text(size = 12, family = "LM Roman 10"),
        # legend.key.width = unit(0.3, 'cm'),
        legend.justification = "left",
        panel.spacing = unit(2, "lines"))

data <- parseTableToDataFrame("temp/variation_deviation_parameters.result")

filteredData <- data %>%
  rowwise() %>%
  mutate(allEvaluations = list(as.numeric(str_split(allEvaluations, ",")[[1]]))) %>%
  mutate(average = mean(allEvaluations, na.rm = TRUE)) %>%
  mutate(evals = if (is.finite(evaluations) & evaluations >= 40000) NA else average) %>%
  mutate(successRate = if (length(unlist(allEvaluations)) < 20) 1 - (length(unlist(allEvaluations)) / 25) else 1 - successRate, actions = (ADD + REMOVE + SPLIT) / (ADD + REMOVE + SPLIT + NOTHING), splits = SPLIT / (ADD + REMOVE + SPLIT + NOTHING)) %>%
  filter(problem == "XOR") %>%
  mutate(primaryAction = if (max(ADD, REMOVE, SPLIT) == 0) "" else if (ADD == Rfast::nth(c(ADD, REMOVE, SPLIT), 1, descending = TRUE)) "Verbindung einfügen" else if (REMOVE == Rfast::nth(c(ADD, REMOVE, SPLIT), 1, descending = TRUE)) "Verbindung entfernen" else if (SPLIT == Rfast::nth(c(ADD, REMOVE, SPLIT), 1, descending = TRUE)) "Verbindung teilen" else "") %>%
  mutate(secondaryAction = if (max(ADD, REMOVE, SPLIT) == 0) "" else if (ADD == Rfast::nth(c(ADD, REMOVE, SPLIT), 2, descending = TRUE)) "Verbindung einfügen" else if (REMOVE == Rfast::nth(c(ADD, REMOVE, SPLIT), 2, descending = TRUE)) "Verbindung entfernen" else if (SPLIT == Rfast::nth(c(ADD, REMOVE, SPLIT), 2, descending = TRUE)) "Verbindung teilen" else "")

ggplot(filteredData, aes(x = d, y = e, fill = actions)) +
  geom_tile() +
  scale_fill_viridis(discrete=FALSE, direction = -1, na.value = "black") +
  facet_wrap(problem ~ .) +
  xlab("δ") +
  ylab("ε") +
  labs(fill = "Modifikationen", size = "Fehlerrrate") +
  # guides(
  #   fill = guide_legend(title = "Primäre Modifikation", order = 1, override.aes=list(shape=FALSE, colour = "transparent")),
  #   shape = guide_legend(title = "Sekundäre Modifikation", order = 2)
  # ) +
  # geom_point(data = subset(filteredData, successRate > 0), aes(size = successRate), color = "gray48") +
  # geom_point(size = 5, color = "gray48") +
  theme(axis.title.x=element_text(size = 18, family = "CMU Classical Serif"),
        axis.title.y=element_text(size = 18, family = "CMU Classical Serif"),
        panel.border = element_rect(colour = "black", fill=NA, size=1),
        plot.title = element_text(family = "LM Roman 10", hjust = 0.5, size=24),
        axis.text = element_text(size = 18, family = "LM Roman 10"),
        strip.text.x = element_text(size = 18, family = "LM Roman 10"),
        strip.background = element_blank(),
        legend.title = element_text(size = 16, family = "LM Roman 10", margin=margin(0,0,5,0)),
        legend.text = element_text(size = 12, family = "LM Roman 10"),
        # legend.key.width = unit(0.3, 'cm'),
        legend.justification = "left",
        panel.spacing = unit(2, "lines"))

df <- data.frame(x = seq(1, 11, by = 0.01)) %>%
  mutate(factor = list(seq(0, 3, by = 0.5))) %>%
  unnest(factor) %>%
  mutate(y = ifelse(x==1, NA, factor * (11 - x) / (x - 1)))

a <- ggplot(df, aes(x = x, y = y, group = factor, color = as.factor(factor))) +
  geom_line() +
  ylim(0, 20) +
  scale_fill_viridis(discrete=TRUE, direction = -1, na.value = "black") +
  scale_x_continuous(breaks = seq(min(0), max(12), by = 1)) +
  labs(color = "ε", y = "Strafe z", x = "Anzahl Werte in Population (k = 11)") +
  theme(axis.title.x=element_text(size = 18, family = "CMU Classical Serif"),
        axis.title.y=element_text(size = 18, family = "CMU Classical Serif"),
        panel.border = element_rect(colour = "black", fill=NA, size=1),
        plot.title = element_text(family = "LM Roman 10", hjust = 0.5, size=24),
        axis.text = element_text(size = 18, family = "LM Roman 10"),
        strip.text.x = element_text(size = 18, family = "LM Roman 10"),
        strip.background = element_blank(),
        legend.title = element_text(size = 16, family = "CMU Classical Serif", margin=margin(0,0,5,0)),
        legend.text = element_text(size = 12, family = "LM Roman 10"),
        # legend.key.width = unit(0.3, 'cm'),
        legend.justification = "left")

ggarrange(b, a, common.legend = FALSE, legend = "right", ncol = 2, align = "v")

data <- parseTableToDataFrame("temp/variation_connection_pheromone.result")

filteredData <- data %>%
  rowwise() %>%
  mutate(allEvaluations = list(as.numeric(str_split(allEvaluations, ",")[[1]]))) %>%
  mutate(average = mean(allEvaluations, na.rm = TRUE)) %>%
  mutate(evals = if (is.finite(evaluations) & evaluations >= 40000) NA else average) %>%
  filter(b %in% c(0.36, 1.07, 5)) %>%
  mutate(b = paste0("β = ", b)) %>%
  # group_by(a, c, problem) %>%
  # slice(which.min(average)) %>%
  # ungroup() %>%
  # rowwise() %>%
  filter(problem != "XOR") %>%
  mutate(successRate = if (length(unlist(allEvaluations)) < 20) 1 - (length(unlist(allEvaluations)) / 25) else 1 - successRate, actions = (ADD + REMOVE + SPLIT) / (ADD + REMOVE + SPLIT + NOTHING), splits = SPLIT / (ADD + REMOVE + SPLIT + NOTHING)) %>%
  # filter(successRate == 0) %>%
  mutate(primaryAction = if (max(ADD, REMOVE, SPLIT) == 0) "" else if (ADD == Rfast::nth(c(ADD, REMOVE, SPLIT), 1, descending = TRUE)) "Verbindung einfügen" else if (REMOVE == Rfast::nth(c(ADD, REMOVE, SPLIT), 1, descending = TRUE)) "Verbindung entfernen" else if (SPLIT == Rfast::nth(c(ADD, REMOVE, SPLIT), 1, descending = TRUE)) "Verbindung teilen" else "") %>%
  mutate(secondaryAction = if (max(ADD, REMOVE, SPLIT) == 0) "" else if (ADD == Rfast::nth(c(ADD, REMOVE, SPLIT), 2, descending = TRUE)) "Verbindung einfügen" else if (REMOVE == Rfast::nth(c(ADD, REMOVE, SPLIT), 2, descending = TRUE)) "Verbindung entfernen" else if (SPLIT == Rfast::nth(c(ADD, REMOVE, SPLIT), 2, descending = TRUE)) "Verbindung teilen" else "") %>%
  mutate(addRate = (REMOVE + SPLIT) / (ADD + REMOVE + SPLIT))


ggplot(subset(filteredData, max(ADD, REMOVE, SPLIT) != 0), aes(x = a, y = c, fill = addRate, shape = secondaryAction)) +
  geom_tile() +
  scale_fill_viridis(discrete=FALSE, direction = -1, na.value = "black") +
  facet_wrap(b ~ .) +
  xlab("α") +
  ylab("γ") +
  labs(fill = "Evaluationen", size = "Fehlerrrate") +
  # guides(
  #   fill = guide_legend(title = "Primäre Modifikation", order = 1, override.aes=list(shape=FALSE, colour = "transparent")),
  #   shape = guide_legend(title = "Sekundäre Modifikation", order = 2)
  # ) +
  # geom_point(data = subset(filteredData, successRate > 0), aes(size = successRate), color = "gray48") +
  # geom_point(size = 5, color = "gray48") +
  theme(axis.title.x=element_text(size = 18, family = "CMU Classical Serif"),
        axis.title.y=element_text(size = 18, family = "CMU Classical Serif"),
        panel.border = element_rect(colour = "black", fill=NA, size=1),
        plot.title = element_text(family = "LM Roman 10", hjust = 0.5, size=24),
        axis.text = element_text(size = 18, family = "LM Roman 10"),
        strip.text.x = element_text(size = 18, family = "LM Roman 10"),
        strip.background = element_blank(),
        legend.title = element_text(size = 16, family = "LM Roman 10", margin=margin(0,0,5,0)),
        legend.text = element_text(size = 12, family = "LM Roman 10"),
        # legend.key.width = unit(0.3, 'cm'),
        legend.justification = "left",
        panel.spacing = unit(2, "lines"))

ggarrange(b, a, common.legend = FALSE, legend = "right", ncol = 2, align = "v")


data <- parseTableToDataFrame("temp/variation_upsilon.result")

data <- data %>%
  rowwise() %>%
  mutate(allEvaluations = list(as.numeric(str_split(allEvaluations, ",")[[1]]))) %>%
  mutate(mean = mean(allEvaluations), q25 = quantile(allEvaluations, probs = 0.25)[[1]], q75 = quantile(allEvaluations, probs = 0.75)[[1]], std = sd(allEvaluations))

ggplot(data, aes(x = upsilon, y = mean, ymin = q25, ymax = q75, color = problem)) +
  # geom_line() +
  geom_smooth(span = 0.1, alpha = 0) +
  # geom_ribbon(alpha = 0.2) +
  xlab("\u03C5") +
  ylab("Evaluation") +
  labs(color = "Problem") +
  scale_x_continuous(breaks = round(seq(0, 1, by = 0.1),1)) +
  scale_y_continuous(breaks = round(seq(0, 2000, by = 250),1)) +
  labs(color="Problem") +
  theme(axis.title.x = element_text(size = 18, family = "CMU Classical Serif"),
        axis.title.y = element_text(size = 18, family = "LM Roman 10"),
        panel.border = element_rect(colour = "black", fill = NA, size = 1),
        plot.title = element_text(family = "LM Roman 10", hjust = 0.5, size = 24, margin = margin(0, 0, 15, 0)),
        axis.text = element_text(size = 18, family = "LM Roman 10"),
        legend.title = element_text(size = 16, family = "LM Roman 10", margin=margin(0,0,5,0)),
        legend.text = element_text(size = 12, family = "LM Roman 10"))

data <- parseTableToDataFrame("temp/variation_solutions_per_iteration.result")

filteredData <- data %>%
  rowwise() %>%
  mutate(allEvaluations = list(as.numeric(str_split(allEvaluations, ",")[[1]]))) %>%
  mutate(average = mean(allEvaluations, na.rm = TRUE), sd = sd(allEvaluations, na.rm = TRUE)) %>%
  mutate(iterations = average / solutionsPerIteration)

ggplot(filteredData, aes(x = solutionsPerIteration)) +
  geom_line(aes(y = sd, linetype = "Evaluationen", color = problem)) +
  # geom_line(aes(y = iterations * 50, linetype = "Iterationen", color = problem)) +
  # geom_smooth(aes(y = average, linetype = "Evaluationen", color = problem), alpha = 0, span = 0.07) +
  # geom_smooth(aes(y = iterations * 10, linetype = "Iterationen", color = problem), alpha = 0, span = 0.07) +
  scale_y_continuous(name = "Evaluationen", sec.axis = sec_axis(~./50, name = "Iterationen")) +
  xlab("Lösungen pro Iteration") +
  labs(color="Problem", linetype="Wert") +
  scale_x_continuous(breaks = round(seq(0, 150, by = 25),1)) +
  theme(axis.title.x=element_text(size = 18, family = "LM Roman 10"),
        axis.title.y=element_text(size = 18, family = "LM Roman 10"),
        panel.border = element_rect(colour = "black", fill=NA, size=1),
        plot.title = element_text(family = "LM Roman 10", hjust = 0.5, size=24, margin=margin(0,0,15,0)),
        axis.text = element_text(size = 18, family = "LM Roman 10"),
        legend.title = element_text(size = 16, family = "LM Roman 10", margin=margin(0,0,5,0)),
        legend.text = element_text(size = 12, family = "LM Roman 10"))

data <- parseTableToDataFrame("temp/variation_update_parameters.result")

filteredData <- data %>%
  rowwise() %>%
  mutate(allEvaluations = list(as.numeric(str_split(allEvaluations, ",")[[1]]))) %>%
  mutate(average = mean(allEvaluations, na.rm = TRUE)) %>%
  # mutate(average = if (length(unlist(allEvaluations)) != 25) NA else average) %>%
  filter(problem != "XOR") %>%
  group_by(populationSize, updatesPerIteration, updateStrategy, problem) %>%
  filter(!(updateStrategy == "Fitness" & elitism == "Elitismus")) %>%
  slice(which.min(evaluations))

ggplot(filteredData, aes(x = populationSize, y = updatesPerIteration, fill = average)) +
  geom_tile() +
  # geom_text(aes(label = elitism)) +
  geom_point(data = subset(filteredData, elitism == "Elitismus"), aes(shape = elitism)) +
  scale_fill_viridis(discrete=FALSE, direction = -1) +
  facet_wrap(updateStrategy ~ .) +
  xlab("Populationsgröße") +
  ylab("Updates pro Iteration") +
  labs(fill="Evaluationen", shape="") +
  theme(axis.title.x=element_text(size = 18, family = "LM Roman 10"),
      axis.title.y=element_text(size = 18, family = "LM Roman 10"),
      panel.border = element_rect(colour = "black", fill=NA, size=1),
      plot.title = element_text(family = "LM Roman 10", hjust = 0.5, size=24),
      axis.text = element_text(size = 18, family = "LM Roman 10"),
      strip.text.x = element_text(size = 18, family = "LM Roman 10"),
      strip.background = element_blank(),
      legend.title = element_text(size = 16, family = "LM Roman 10", margin=margin(0,0,10,0)),
      legend.text = element_text(size = 12, family = "LM Roman 10"))

# sum(filteredData[filteredData$updateStrategy == "Topologiegruppe", "rank"]$rank)
#
# topology <- filteredData[filteredData$updateStrategy == "Topologiegruppe" & filteredData$updatesPerIteration > 1, "evaluations"]$evaluations
# age <- filteredData[filteredData$updateStrategy == "Alter" & filteredData$updatesPerIteration > 1, "evaluations"]$evaluations
# similarity <- filteredData[filteredData$updateStrategy == "Ähnlichkeit" & filteredData$updatesPerIteration > 1, "evaluations"]$evaluations
#
# mean(topology)
# mean(age)
# mean(similarity)
#
# data.frame(value = age) %>%
#   t_test(value ~ 1, mu = mean(similarity), detailed = TRUE)

# data <- parseTableToDataFrame(filepath <- "temp/variation.result")
#
# filteredData <- data %>%
#   group_by(problem) %>%
#   mutate(rank = rank(evaluations, ties.method = "first")) %>%
#   group_by(deviation) %>%
#   summarise(rank_sum = sum(rank), evaluations = mean(evaluations), neurons = mean(neurons), connections = mean(connections), successRate = mean(successRate))


xor_neat_old <- c(2267,2326,2948,5099,2623,4099,7096,8225,5517,9503,4692,3675,4488,1444,12788,4232,6517,4317,12101,9670,4516,1749,7244,4194,8627,2136,4072,6565,3186,7688,4973,2318,5284,9458,12379,5082,1930,4063,1586,3845,4544,5255,3435,3437,6179,1646,5061,3645,5461,4415)
xor_neat_neuron_old <- c(1, 2,2,4,2,2,1,3,5,3,2,2,3,2,5,3,4,2,4,3,2,2,2,2,3,2,2,3,1,4,4,1,2,3,3,3,2,1,3,2,1,3,4,2,2,1,3,1,2,2)

xor_irace_old <- c(476,419,199,223,112,567,483,213,507,377,596,743,391,461,602,1118,220,597,515,516,283,260,205,151,461,788,358,269,651,135,515,351,295,532,284,686,591,229,549,402,749,300,727,741,526,510,418,768,755)
xor_irace_neuron_old <- c(10,6,4,4,2,10,13,6,10,8,10,8,5,9,8,7,6,9,6,7,7,3,3,4,8,8,7,4,8,3,8,6,4,7,5,8,8,5,8,4,9,5,9,7,7,8,7,7,9)

cart_neat_old <- c(13036,37019,62134,24299,106111,82486,34696,23047,12268,22040,56088,24299,13380,41087,23018,17049,16083,33259,21225,8308,18026,16067,12027,13187,18062,50078,31123,27633,26009,11322,26061,38010,47256,31503,37725,14021,11124,11021,50452,53504,15615,68022,36259,54121,24389,53735,23137,16019,16021,97112)
cart_neat_neuron_old <- c(0,0,3,2,4,4,2,0,0,0,1,0,0,2,0,0,0,1,1,0,0,0,0,0,0,1,1,1,0,0,0,0,2,1,1,0,0,0,1,3,0,2,2,1,1,1,0,0,0,5)

cart_irace_old <- c(2561,1285,22415,1448,2930,5233,11848,12017,3035,23968,2841,3259,2045,4357,4007,1588,1345,2685,1099,3906,1018,1148,6037,2553,2393,2344,2962,2447,2368,2563,2964,5615,2693,11847,1878,4534)
cart_irace_neuron_old <- c(0,0,25,0,2,0,0,17,0,33,4,0,0,6,0,0,0,2,0,5,0,0,13,0,0,0,0,0,0,0,0,0,0,12,0,0)

xor_neat_new <- c(6600.0,11400.0,16200.0,9900.0,70800.0,10800.0,19350.0,8550.0,9000.0,7500.0,8100.0,32850.0,58950.0,9900.0,35400.0,9450.0,4650.0,17250.0,6600.0,40200.0,43200.0,59250.0,4200.0,39600.0,10800.0,4200.0,21000.0,6150.0,21900.0,9900.0,5400.0,20550.0,9450.0,5850.0,12150.0,14700.0,15450.0)
xor_neat_neuron_new <- c(2.0,2.0,2.0,1.0,1.0,2.0,3.0,1.0,1.0,1.0,1.0,1.0,1.0,2.0,2.0,1.0,1.0,1.0,2.0,5.0,3.0,3.0,4.0,1.0,1.0,2.0,3.0,1.0,1.0,1.0,1.0,4.0,3.0,1.0,1.0,1.0,3.0)

xor_irace_new <- c(550.0,580.0,400.0,295.0,1010.0,1115.0,1310.0,495.0,415.0,710.0,485.0,565.0,845.0,540.0,365.0,885.0,375.0,680.0,205.0,510.0,955.0,640.0,195.0,1200.0,510.0,425.0,425.0,625.0,1345.0,240.0,1215.0,195.0,605.0,370.0,290.0,280.0,370.0,335.0,890.0,510.0,125.0,165.0,880.0,575.0,175.0,430.0,385.0,1185.0,290.0,615.0)
xor_irace_neuron_new <- c(4.0,7.0,4.0,4.0,7.0,6.0,12.0,8.0,5.0,7.0,4.0,5.0,7.0,6.0,6.0,5.0,7.0,8.0,3.0,4.0,9.0,5.0,2.0,8.0,5.0,3.0,6.0,5.0,7.0,5.0,7.0,4.0,5.0,6.0,6.0,4.0,7.0,4.0,8.0,3.0,2.0,4.0,7.0,5.0,3.0,3.0,6.0,10.0,2.0,5.0)

# generalization: 0.42 (0.09)
cart_neat_new <- c(13000.0,18000.0,15000.0,15000.0,16000.0,19000.0,35000.0,21000.0,17000.0,100000.0,15000.0,100000.0,100000.0,100000.0,100000.0,100000.0,26000.0,100000.0,100000.0,100000.0,100000.0,100000.0,100000.0,36000.0,100000.0,100000.0,100000.0,100000.0,100000.0,20000.0,100000.0,100000.0,100000.0,100000.0,100000.0,100000.0,100000.0,84000.0,100000.0,100000.0,100000.0,100000.0,100000.0,100000.0,100000.0,100000.0,100000.0,100000.0,100000.0,100000.0)
cart_neat_neuron_new <- c(0.0,2.0,0.0,1.0,2.0,1.0,0.0,1.0,2.0,1.0,2.0,1.0,2.0,3.0,1.0,1.0,1.0,2.0,2.0,0.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,0.0,1.0,0.0,2.0,1.0,2.0,2.0,1.0,2.0,2.0,2.0,2.0,1.0,4.0,2.0,2.0,2.0,3.0,3.0,2.0,4.0,1.0,1.0)

# generalization: 0.43
cart_irace_new <- c(576.0,816.0,984.0,840.0,1944.0,1776.0,1488.0,1800.0,432.0,1416.0,3312.0,1872.0,2616.0,4056.0,2088.0,1272.0,888.0,11952.0,2448.0,11688.0,480.0,4776.0,12096.0,672.0,792.0,1176.0,20664.0,2160.0,2424.0,16848.0,18216.0,2064.0,16608.0,12048.0,23088.0,61680.0,4152.0,39960.0,29304.0,100008,100008,89448.0,100008,22848.0,49320.0,33840.0,95856.0,84960.0,75744.0,67680.0)
cart_irace_neuron_new <- c(0.0,2.0,0.0,1.0,2.0,1.0,0.0,1.0,2.0,1.0,2.0,1.0,2.0,3.0,1.0,1.0,1.0,2.0,2.0,0.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,0.0,1.0,0.0,2.0,1.0,2.0,2.0,1.0,2.0,2.0,2.0,2.0,1.0,4.0,2.0,2.0,2.0,3.0,3.0,2.0,4.0,1.0,1.0)

df <- data.frame(evaluations = cart_irace_new, neurons = cart_irace_neuron_new) %>%
  filter(evaluations < 100000)

describeBy(df$evaluations)
describeBy(df$neuron)

df <- data.frame(name = "XOR Neat", data = xor_neat_old, neuron = xor_neat_neuron_old)
df <- rbind(df, data.frame(name = "XOR Neat (IRACE)", data = xor_irace_old, neuron = xor_irace_neuron_old))
df <- rbind(df, data.frame(name = "Inverses Pendel Neat", data = cart_neat_old, neuron = cart_neat_neuron_old))
df <- rbind(df, data.frame(name = "Inverses Pendel Neat (IRACE)", data = cart_irace_old, neuron = cart_irace_neuron_old))
df <- rbind(df, data.frame(name = "XOR ANJI", data = xor_neat_new, neuron = xor_neat_neuron_new))
df <- rbind(df, data.frame(name = "XOR ANJI (IRACE)", data = xor_irace_new, neuron = xor_irace_neuron_new))
df <- rbind(df, data.frame(name = "Inverses Pendel ANJI", data = cart_neat_new, neuron = cart_neat_neuron_new))
df <- rbind(df, data.frame(name = "Inverses Pendel ANJI (IRACE)", data = cart_irace_new, neuron = cart_irace_neuron_new))

testData <- data.frame(evaluation = xor_irace_old, algorithm = "old")
testData <- rbind(testData, data.frame(evaluation = xor_irace_new, algorithm = "new"))

testData %>%
  rstatix::wilcox_test(evaluation~algorithm, detailed = TRUE, exact = TRUE, alternative = "g")

















