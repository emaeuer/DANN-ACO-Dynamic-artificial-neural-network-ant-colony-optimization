library(ggplot2)
library(ggpubr)
library(hrbrthemes)
library(viridis)
library(dplyr)
library(tidyr)
library(stringr)
library(ggExtra)
library(psych)

setClass("Configuration", slots=list(runNumber="numeric", maxFitness="numeric", name="character"))

extractIterationData <- function(filepath, name) {
  df <- data.frame()
  con <- file(filepath, "r")

  currentRow <- data.frame(runID = 0, evaluation = 0)

  configuration <- new("Configuration")

  while (TRUE) {
    line <- readLines(con, n = 1)
    if (length(line) == 0) {
      break
    }

    if (grepl("^CONFIGURATION\\.OPTIMIZATION_CONFIGURATION\\.METHOD_NAME", line)) {
      if (!missing(name)) {
        configuration@name <- name
      } else {
        configuration@name <- str_split(line, "=", 2)[[1]][2]
      }
    } else if (grepl("^CONFIGURATION\\.OPTIMIZATION_CONFIGURATION\\.MAX_FITNESS_SCORE", line)) {
      configuration@maxFitness <- as.numeric(str_split(line, "=", 2)[[1]][2])
    } else if (grepl("^CONFIGURATION\\.OPTIMIZATION_CONFIGURATION\\.NUMBER_OF_RUNS", line)) {
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
    #            else if (grepl("^GENERAL_STATE\\.RUN_\\d*?\\.PACO_RUN\\.CONNECTION_WEIGHTS_SCATTERED", line)) {
    #   value <- str_split(line, " = ", 2)[[1]][2]
    #   singleLists <- str_split(value, "\\]\\,\\[")
    #   singleLists <- str_replace_all(unlist(singleLists), "\\[|\\]*", "")
    #   resultList <- list()
    #   for (l in singleLists) {
    #     l <- unlist(str_split(l, ":", 2))
    #     if (l[2] != "") {
    #       resultList[l[1]] <- list(as.numeric(unlist(str_split(l[2], ", "))))
    #     }
    #   }
    #   currentRow$weightDistribution <- list(resultList)
    # }
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

  for (result in list(...)) {
    data <- result[["data"]]
    configuration <- result[["configuration"]]

    filteredData <- data %>%
      rowwise() %>%
      mutate(fitness = max(unlist(fitnessValues))) %>%
      select(runID, evaluation, fitness) %>%
      group_by(evaluation) %>%
      summarise(fitness = (sum(fitness) + (configuration@runNumber - n()) * configuration@maxFitness) / configuration@runNumber) %>%
      mutate(type = configuration@name, general = if_else(endsWith(type, "(Allgemein)"), "Allgemein", "Problemspezifisch")) %>%
      mutate(type = if_else(startsWith(type, "DANN-ACO"), "DANN-ACO", "NEAT"))

    df <- rbind(df, filteredData)
  }

  return(ggplot(df, aes(x = evaluation, y = fitness, color=type, linetype = general)) +
           geom_line(size = 1) +
           # scale_linetype(labels = c("solid", "dashed")) +
           scale_linetype_manual(values = c("dashed", "solid")) +
           xlab("Evaluation") +
           ylab("Fitness") +
           scale_x_continuous(breaks = seq(0, 15000, by = 2500)) +
           guides(linetype = guide_legend(title = "Konfiguration"),
                  color = guide_legend(title = "Algorithmus")) +
           theme(axis.title.x = element_text(size = 18, family = "LM Roman 10"),
                 axis.title.y = element_text(size = 18, family = "LM Roman 10"),
                 panel.border = element_rect(colour = "black", fill = NA, size = 1),
                 plot.title = element_text(family = "LM Roman 10", hjust = 0.5, size = 24, margin = margin(0, 0, 15, 0)),
                 axis.text = element_text(size = 18, family = "LM Roman 10"),
                 legend.title = element_text(size = 16, family = "LM Roman 10", margin=margin(0,0,5,0)),
                 legend.text = element_text(size = 12, family = "LM Roman 10"),
                 strip.text.y = element_text(size = 18, family = "LM Roman 10"),
                 panel.spacing = unit(2, "lines"),
                 strip.background = element_blank()))
}

boxPlotEvaluations <- function(...) {
  df <- data.frame()

  for (result in list(...)) {
    data <- result[["data"]]
    configuration <- result[["configuration"]]

    filteredData <- data %>%
      rowwise() %>%
      group_by(runID) %>%
      summarise(fitness = max(evaluation), hiddenNodes = max(unlist(hiddenNodes)), connections = max(unlist(connections))) %>%
      mutate(type = configuration@name, general = if_else(endsWith(type, "(Allgemein)"), "Allgemein", "Problemspezifisch")) %>%
      mutate(type = if_else(startsWith(type, "DANN-ACO"), "DANN-ACO", "NEAT"))

    df <- rbind(df, filteredData)
  }

  return(ggplot(df, aes(x = stringr::str_wrap(type, 8), y = fitness, fill = type)) +
           facet_grid(general~.) +
           geom_violin(alpha=0.75) +
           geom_boxplot(width=0.25, outlier.alpha = 0) +
           scale_y_continuous(breaks = seq(0, 15000, by = 2500)) +
           xlab("") +
           ylab("Evaluation") +
           coord_flip() +
           theme(axis.title.x = element_text(size = 18, family = "LM Roman 10"),
                 axis.title.y = element_text(size = 18, family = "LM Roman 10"),
                 panel.border = element_rect(colour = "black", fill = NA, size = 1),
                 plot.title = element_text(family = "LM Roman 10", hjust = 0.5, size = 24, margin = margin(0, 0, 15, 0)),
                 axis.text = element_text(size = 18, family = "LM Roman 10"),
                 axis.text.y = element_blank(),
                 axis.ticks.y = element_blank(),
                 legend.text = element_text(size = 18, family = "LM Roman 10"),
                 legend.title = element_blank(),
                 legend.position = "right",
                 strip.text.y = element_text(size = 18, family = "LM Roman 10"),
                 strip.background = element_blank()))
}

trendOfWeights <- function(...) {
  df <- data.frame()

  for (result in list(...)) {
    data <- result[["data"]]
    configuration <- result[["configuration"]]

    filteredData <- data %>%
      rowwise() %>%
      filter(evaluation < 1750) %>%
      select(evaluation, weightDistribution) %>%
      mutate(connectionID = list(names(weightDistribution))) %>%
      unnest(connectionID) %>%
      rowwise() %>%
      mutate(type = configuration@name) %>%
      filter((connectionID %in% c(6,15) & type == "Keine Übernahme") | (connectionID %in% c(1,2) & type == "Mit Übernahme")) %>%
      mutate(weights = list(weightDistribution[[connectionID]])) %>%
      mutate(usage = length(weights), mean = median(weights), q25 = quantile(weights, probs = 0.25)[[1]], q75 = quantile(weights, probs = 0.75)[[1]], std = if_else(length(weights) == 1, 0, sd(weights))) %>%
      select(evaluation, connectionID, mean, std, type, q25, q75, usage) %>%
      mutate(connectionID = if (connectionID %in% c(6, 2)) "Eingabe 2" else if (connectionID %in% c(1, 15)) "Eingabe 1" else " ")

    df <- rbind(df, filteredData)
  }

  plot <- ggplot(df, aes(x = evaluation, y = mean, ymin = q25, ymax = q75, group = connectionID, color = connectionID, fill = connectionID)) +
    geom_line() +
    geom_ribbon(alpha = 0.2) +
    facet_grid(type ~ .) +
    xlab("Evaluation") +
    ylab("Verbindungsgewicht") +
    # geom_line(mapping = aes(y = usage), linetype = "dashed") +
    theme(axis.title.x = element_text(size = 18, family = "LM Roman 10"),
          axis.title.y = element_text(size = 18, family = "LM Roman 10"),
          panel.border = element_rect(colour = "black", fill = NA, size = 1),
          plot.title = element_text(family = "LM Roman 10", hjust = 0.5, size = 24, margin = margin(0, 0, 15, 0)),
          axis.text = element_text(size = 18, family = "LM Roman 10"),
          legend.title = element_blank(),
          legend.text = element_text(size = 18, family = "LM Roman 10"),
          strip.text.y = element_text(size = 18, family = "LM Roman 10"),
          panel.spacing = unit(2, "lines"),
          strip.background = element_blank())

  return(plot)
}

drawQuantities <- function(value_list, name_list) {
  df <- data.frame()

  for (i in seq_along(value_list)) {
    temp_df <- data.frame(values = value_list[[i]])
    temp_df <- temp_df %>%
      mutate(Variante = name_list[[i]])

    df <- rbind(df, temp_df)
  }

  df <- df %>%
    count(values, Variante, .drop = FALSE) %>%
    rename(count = n)

  df <- rbind(df, list(values = 4, count = 0, Variante = "Ablation"))
  df <- rbind(df, list(values = 5, count = 0, Variante = "Ablation"))

  return(ggplot(df, aes(x = values, y = count, fill = Variante)) +
           geom_bar(position="dodge", stat="identity") +
           xlab("Verdeckte Neurone") +
           ylab("Anzahl") +
           theme(axis.title.x = element_text(size = 18, family = "LM Roman 10"),
                 axis.title.y = element_text(size = 18, family = "LM Roman 10"),
                 panel.border = element_rect(colour = "black", fill = NA, size = 1),
                 plot.title = element_text(family = "LM Roman 10", hjust = 0.5, size = 24, margin = margin(0, 0, 15, 0)),
                 axis.text = element_text(size = 18, family = "LM Roman 10"),
                 legend.text = element_text(size = 18, family = "LM Roman 10"),
                 legend.title = element_blank(),
                 legend.position = "right"))
}

dannacoResultSpecial <- extractIterationData("C:/Users/emaeu/IdeaProjects/ParticleEnvironment/temp/dannaco_xor_recurrent_special.txt", "DANN-ACO")
dannacoResultGeneral <- extractIterationData("C:/Users/emaeu/IdeaProjects/ParticleEnvironment/temp/dannaco_xor_recurrent_general.txt", "DANN-ACO (Allgemein)")
neatResultSpecial <- extractIterationData("C:/Users/emaeu/IdeaProjects/ParticleEnvironment/temp/neat_xor_recurrent_special.txt", "NEAT")
neatResultGeneral <- extractIterationData("C:/Users/emaeu/IdeaProjects/ParticleEnvironment/temp/neat_xor_recurrent_general.txt", "NEAT (Allgemein)")

#drawRunSummary("C:\\Users\\emaeu\\IdeaProjects\\ParticleEnvironment\\temp\\important_runs\\aco_xor_non_recurrent.txt", 5)

a <- compareResultsFromDifferentFiles(dannacoResultSpecial, neatResultSpecial, dannacoResultGeneral, neatResultGeneral)

b <- boxPlotEvaluations(dannacoResultSpecial, neatResultSpecial, dannacoResultGeneral, neatResultGeneral)

ggarrange(a, b, nrow = 1, common.legend = TRUE, legend = "right")

dannacoEvaluationsSpecial <- c(252.0,312.0,952.0,462.0,292.0,272.0,192.0,232.0,402.0,492.0,272.0,232.0,142.0,1772.0,462.0,512.0,1432.0,382.0,132.0,242.0,202.0,192.0,352.0,402.0,672.0,1172.0,242.0,1042.0,692.0,592.0,282.0,622.0,912.0,292.0,162.0,902.0,302.0,1262.0,252.0,2432.0,122.0,292.0,572.0,282.0,162.0,232.0,162.0,232.0,762.0,612.0,302.0,372.0,1312.0,1252.0,192.0,232.0,82.0,442.0,492.0,342.0,282.0,442.0,432.0,712.0,1722.0,252.0,62.0,112.0,192.0,552.0,1132.0,282.0,942.0,442.0,222.0,672.0,192.0,212.0,612.0,422.0,192.0,992.0,82.0,432.0,1612.0,362.0,122.0,622.0,3142.0,502.0,212.0,452.0,292.0,142.0,212.0,322.0,252.0,3242.0,982.0,182.0,702.0,302.0,1032.0,272.0,122.0,112.0,202.0,692.0,482.0,342.0,752.0,392.0,222.0,1772.0,212.0,172.0,302.0,142.0,2572.0,722.0,762.0,352.0,242.0,182.0,632.0,802.0,302.0,222.0,1192.0,262.0,452.0,242.0,1002.0,492.0,192.0,692.0,362.0,642.0,402.0,212.0,132.0,282.0,172.0,182.0,912.0,152.0,1052.0,432.0,182.0,1162.0,282.0,122.0,162.0,202.0,432.0,352.0,192.0,812.0,992.0,552.0,82.0,1012.0,322.0,562.0,272.0,212.0,642.0,642.0,452.0,162.0,152.0,572.0,392.0,162.0,1572.0,222.0,162.0,542.0,622.0,362.0,202.0,242.0,422.0,702.0,992.0,342.0,852.0,372.0,1482.0,202.0,442.0,1022.0,2602.0,1552.0,102.0,342.0,722.0,152.0,512.0,122.0)
dannacoNeuronsSpecial <- c(2.0,1.0,5.0,1.0,3.0,2.0,2.0,2.0,4.0,3.0,3.0,3.0,2.0,7.0,2.0,4.0,3.0,4.0,1.0,2.0,1.0,1.0,2.0,2.0,6.0,5.0,3.0,5.0,3.0,2.0,1.0,3.0,6.0,2.0,2.0,4.0,2.0,3.0,2.0,9.0,2.0,3.0,2.0,2.0,2.0,2.0,1.0,2.0,4.0,4.0,2.0,2.0,5.0,1.0,2.0,1.0,1.0,3.0,2.0,5.0,3.0,4.0,2.0,6.0,10.0,3.0,1.0,1.0,2.0,2.0,5.0,2.0,4.0,3.0,2.0,3.0,1.0,2.0,2.0,2.0,1.0,4.0,1.0,2.0,3.0,4.0,1.0,6.0,10.0,3.0,3.0,3.0,2.0,3.0,3.0,4.0,3.0,16.0,5.0,1.0,6.0,2.0,6.0,2.0,2.0,1.0,2.0,2.0,3.0,2.0,3.0,3.0,2.0,7.0,1.0,2.0,2.0,1.0,13.0,8.0,5.0,2.0,3.0,2.0,3.0,3.0,2.0,2.0,4.0,1.0,4.0,3.0,2.0,3.0,2.0,4.0,3.0,4.0,2.0,1.0,1.0,2.0,2.0,1.0,2.0,2.0,6.0,3.0,1.0,7.0,1.0,1.0,2.0,3.0,2.0,2.0,2.0,6.0,7.0,3.0,1.0,5.0,4.0,3.0,2.0,2.0,3.0,4.0,3.0,1.0,2.0,3.0,3.0,1.0,5.0,2.0,2.0,5.0,2.0,3.0,1.0,2.0,2.0,5.0,6.0,3.0,6.0,1.0,4.0,2.0,2.0,5.0,4.0,9.0,2.0,1.0,3.0,1.0,4.0,2.0)
dannacoEvaluationsGeneral <- c(848.0,1109.0,983.0,830.0,1676.0,4790.0,4466.0,6626.0,911.0,2657.0,839.0,542.0,6464.0,443.0,776.0,731.0,3296.0,5798.0,9956.0,884.0,3017.0,632.0,920.0,7733.0,731.0,6140.0,2405.0,2063.0,524.0,1676.0,1055.0,461.0,497.0,2333.0,956.0,5600.0,1928.0,1262.0,668.0,4547.0,1172.0,1235.0,965.0,3413.0,3359.0,7517.0,389.0,542.0,272.0,3413.0,1586.0,569.0,983.0,848.0,1730.0,209.0,1262.0,416.0,695.0,1487.0,515.0,605.0,1064.0,803.0,2945.0,650.0,902.0,641.0,1352.0,8201.0,929.0,1028.0,5366.0,2126.0,893.0,2279.0,1145.0,1262.0,1334.0,6158.0,749.0,5555.0,2108.0,2018.0,2864.0,1532.0,1154.0,7481.0,1163.0,1712.0,578.0,8984.0,3602.0,1874.0,1028.0,1199.0,767.0,1100.0,1730.0,9038.0,1343.0,1217.0,380.0,2495.0,254.0,803.0,6536.0,614.0,6779.0,1055.0,1064.0,1586.0,1397.0,7337.0,443.0,1433.0,1082.0,947.0,785.0,1316.0,1874.0,2099.0,1181.0,1523.0,4412.0,3602.0,13295.0,1379.0,290.0,1892.0,2648.0,9497.0,1154.0,884.0,1010.0,2162.0,2198.0,299.0,3593.0,929.0,3566.0,1091.0,947.0,641.0,479.0,623.0,2495.0,569.0,3827.0,785.0,1190.0,713.0,1937.0,641.0,776.0,2837.0,776.0,614.0,4097.0,758.0,983.0,470.0,992.0,920.0,1244.0,1325.0,5438.0,5609.0,533.0,1271.0,1478.0,4655.0,3089.0,1289.0,9290.0,2018.0,1136.0,2099.0,893.0,929.0,830.0,668.0,4331.0,722.0,1739.0,254.0,12782.0,6239.0,1316.0,497.0,839.0,803.0,992.0,1145.0,2360.0,965.0,1118.0,344.0,1181.0,668.0)
dannacoNeuronsGeneral <- c(2.0,3.0,1.0,1.0,2.0,5.0,1.0,4.0,3.0,1.0,2.0,1.0,2.0,1.0,1.0,2.0,4.0,3.0,4.0,2.0,5.0,1.0,1.0,5.0,1.0,5.0,2.0,2.0,2.0,3.0,3.0,1.0,1.0,1.0,4.0,4.0,2.0,2.0,2.0,1.0,1.0,1.0,1.0,2.0,4.0,1.0,1.0,1.0,1.0,2.0,4.0,2.0,2.0,3.0,3.0,1.0,3.0,1.0,2.0,2.0,1.0,1.0,1.0,2.0,1.0,2.0,1.0,2.0,2.0,8.0,2.0,2.0,7.0,2.0,2.0,2.0,3.0,2.0,1.0,2.0,1.0,5.0,1.0,2.0,3.0,1.0,2.0,2.0,2.0,1.0,1.0,8.0,1.0,6.0,2.0,2.0,1.0,2.0,1.0,2.0,2.0,1.0,1.0,1.0,2.0,1.0,4.0,1.0,5.0,1.0,2.0,2.0,2.0,2.0,1.0,2.0,1.0,3.0,1.0,2.0,2.0,2.0,3.0,2.0,2.0,2.0,1.0,2.0,1.0,1.0,2.0,2.0,2.0,1.0,4.0,3.0,3.0,1.0,5.0,1.0,1.0,2.0,1.0,2.0,2.0,3.0,3.0,2.0,3.0,1.0,1.0,2.0,4.0,1.0,1.0,4.0,1.0,1.0,3.0,2.0,2.0,2.0,2.0,1.0,4.0,1.0,2.0,4.0,1.0,2.0,1.0,3.0,3.0,1.0,3.0,2.0,2.0,3.0,2.0,1.0,3.0,2.0,2.0,2.0,4.0,1.0,2.0,4.0,2.0,1.0,1.0,1.0,1.0,1.0,2.0,1.0,2.0,1.0,1.0,2.0)

neatEvaluationsSpecial <- c(770.0,550.0,155.0,785.0,690.0,510.0,315.0,305.0,1755.0,250.0,1285.0,555.0,990.0,235.0,890.0,715.0,335.0,750.0,810.0,650.0,935.0,385.0,415.0,220.0,590.0,1035.0,650.0,550.0,70.0,250.0,585.0,265.0,270.0,600.0,930.0,1235.0,380.0,1060.0,70.0,400.0,750.0,455.0,275.0,785.0,800.0,430.0,180.0,375.0,835.0,575.0,1150.0,365.0,2025.0,805.0,220.0,465.0,320.0,350.0,765.0,315.0,920.0,570.0,265.0,725.0,170.0,235.0,775.0,415.0,755.0,350.0,740.0,300.0,770.0,500.0,950.0,605.0,615.0,300.0,505.0,590.0,1210.0,805.0,185.0,915.0,230.0,1000.0,425.0,1160.0,1640.0,550.0,340.0,1170.0,665.0,870.0,415.0,2780.0,420.0,280.0,555.0,240.0,345.0,560.0,455.0,555.0,620.0,615.0,910.0,890.0,495.0,155.0,835.0,250.0,845.0,410.0,120.0,985.0,1295.0,1530.0,305.0,175.0,200.0,275.0,310.0,770.0,1360.0,780.0,1055.0,525.0,660.0,345.0,615.0,360.0,215.0,490.0,1130.0,765.0,955.0,145.0,2345.0,280.0,620.0,260.0,255.0,655.0,1585.0,565.0,410.0,1365.0,485.0,1775.0,345.0,285.0,915.0,850.0,300.0,350.0,690.0,300.0,645.0,255.0,230.0,280.0,585.0,945.0,255.0,680.0,540.0,410.0,615.0,530.0,1525.0,340.0,570.0,595.0,295.0,745.0,390.0,515.0,840.0,180.0,1160.0,555.0,575.0,400.0,450.0,680.0,1420.0,1135.0,510.0,375.0,635.0,460.0,530.0,645.0,600.0,445.0,765.0,1500.0,345.0,455.0)
neatNeuronsSpecial <- c(8.0,5.0,1.0,6.0,3.0,5.0,5.0,3.0,6.0,4.0,8.0,5.0,6.0,3.0,8.0,6.0,4.0,5.0,11.0,6.0,7.0,6.0,4.0,4.0,6.0,9.0,7.0,6.0,4.0,5.0,4.0,4.0,2.0,6.0,6.0,7.0,3.0,9.0,2.0,7.0,3.0,5.0,3.0,8.0,5.0,6.0,2.0,6.0,8.0,5.0,7.0,4.0,11.0,5.0,2.0,2.0,4.0,5.0,6.0,5.0,4.0,7.0,4.0,6.0,2.0,3.0,8.0,4.0,7.0,5.0,5.0,3.0,5.0,6.0,8.0,6.0,9.0,3.0,5.0,6.0,7.0,4.0,5.0,8.0,2.0,8.0,5.0,3.0,9.0,4.0,4.0,7.0,3.0,8.0,9.0,10.0,4.0,3.0,5.0,5.0,3.0,5.0,5.0,6.0,7.0,4.0,8.0,4.0,8.0,4.0,7.0,3.0,7.0,3.0,1.0,5.0,6.0,8.0,4.0,2.0,3.0,4.0,3.0,7.0,6.0,8.0,5.0,6.0,6.0,3.0,4.0,4.0,4.0,5.0,7.0,5.0,5.0,2.0,14.0,3.0,5.0,3.0,4.0,7.0,14.0,5.0,4.0,12.0,5.0,13.0,5.0,5.0,5.0,6.0,5.0,3.0,6.0,7.0,5.0,3.0,3.0,5.0,4.0,9.0,3.0,4.0,6.0,3.0,6.0,7.0,6.0,3.0,2.0,4.0,5.0,4.0,2.0,2.0,7.0,3.0,6.0,9.0,6.0,4.0,2.0,7.0,9.0,6.0,7.0,5.0,6.0,8.0,6.0,10.0,8.0,4.0,4.0,7.0,3.0,4.0)
neatEvaluationsGeneral <- c(1368.0,3240.0,600.0,1488.0,2208.0,1248.0,672.0,1368.0,2640.0,456.0,864.0,1320.0,2544.0,1440.0,432.0,816.0,504.0,408.0,3840.0,1632.0,696.0,648.0,1128.0,960.0,1200.0,2520.0,1248.0,1488.0,1824.0,696.0,720.0,1128.0,1752.0,1248.0,1008.0,2064.0,1008.0,984.0,960.0,2784.0,1320.0,312.0,3072.0,288.0,600.0,1008.0,2136.0,1608.0,1056.0,2112.0,1320.0,1608.0,816.0,984.0,3792.0,192.0,1056.0,1296.0,720.0,3312.0,1800.0,1368.0,1416.0,2736.0,1392.0,504.0,1056.0,816.0,2184.0,1128.0,600.0,3480.0,600.0,696.0,8136.0,1968.0,5784.0,240.0,792.0,1392.0,912.0,1176.0,480.0,648.0,2232.0,1464.0,960.0,648.0,11376.0,1488.0,3072.0,5712.0,3024.0,984.0,1176.0,1848.0,2160.0,2280.0,936.0,2616.0,2880.0,5280.0,4032.0,768.0,1656.0,408.0,3552.0,1704.0,1368.0,2736.0,1032.0,816.0,1848.0,1368.0,624.0,1656.0,1680.0,3960.0,648.0,744.0,1200.0,1248.0,1632.0,552.0,1920.0,1104.0,3384.0,528.0,792.0,720.0,1680.0,696.0,2712.0,1800.0,2592.0,768.0,1056.0,1920.0,2592.0,1128.0,432.0,1944.0,1344.0,456.0,1728.0,3192.0,864.0,6816.0,1272.0,1680.0,1104.0,3216.0,576.0,1032.0,840.0,744.0,3240.0,1032.0,4152.0,4752.0,1944.0,9960.0,1056.0,912.0,816.0,6192.0,1968.0,2400.0,3096.0,528.0,1200.0,1824.0,960.0,2208.0,1152.0,1104.0,960.0,1656.0,768.0,624.0,2352.0,1680.0,2184.0,672.0,360.0,312.0,648.0,1512.0,2544.0,1032.0,1128.0,3696.0,984.0,1224.0,984.0,1176.0,2328.0,600.0,4416.0,2136.0)
neatNeuronsGeneral <- c(4.0,3.0,2.0,1.0,3.0,1.0,1.0,4.0,1.0,1.0,1.0,1.0,2.0,1.0,1.0,1.0,2.0,1.0,4.0,5.0,1.0,1.0,2.0,2.0,1.0,2.0,5.0,2.0,2.0,1.0,2.0,2.0,1.0,2.0,2.0,4.0,3.0,1.0,2.0,2.0,1.0,1.0,2.0,1.0,3.0,2.0,3.0,3.0,1.0,2.0,1.0,2.0,2.0,2.0,3.0,1.0,2.0,3.0,2.0,4.0,2.0,2.0,2.0,2.0,3.0,1.0,1.0,1.0,4.0,2.0,3.0,1.0,1.0,1.0,4.0,1.0,2.0,1.0,1.0,1.0,2.0,1.0,2.0,2.0,3.0,3.0,3.0,1.0,5.0,1.0,3.0,2.0,1.0,1.0,1.0,2.0,3.0,2.0,2.0,1.0,7.0,3.0,2.0,1.0,1.0,1.0,4.0,3.0,2.0,2.0,2.0,1.0,1.0,1.0,1.0,2.0,2.0,2.0,1.0,1.0,2.0,3.0,3.0,1.0,2.0,2.0,3.0,1.0,1.0,2.0,3.0,2.0,1.0,2.0,1.0,2.0,1.0,1.0,4.0,3.0,2.0,3.0,2.0,1.0,1.0,1.0,2.0,2.0,2.0,3.0,2.0,6.0,1.0,2.0,2.0,1.0,1.0,2.0,1.0,5.0,2.0,2.0,1.0,1.0,1.0,2.0,1.0,2.0,1.0,1.0,1.0,5.0,1.0,1.0,2.0,2.0,1.0,4.0,2.0,2.0,1.0,1.0,1.0,2.0,3.0,1.0,2.0,1.0,2.0,1.0,1.0,2.0,1.0,1.0,3.0,1.0,4.0,2.0,3.0,2.0)

# Test if data is normal distibuted p > 0.05 or points are alligned along the line
# if the data is normal distributed use t test else wilcoxon-test
shapiro_test(dannacoEvaluationsSpecial)
ggqqplot(dannacoEvaluationsSpecial - mean(dannacoEvaluationsSpecial))

describeBy(dannacoEvaluationsSpecial)
describeBy(neatEvaluationsSpecial)

table(dannacoNeuronsGeneral)
table(neatNeuronsGeneral)

mean(neatEvaluationsSpecial) / mean(dannacoEvaluationsSpecial)

testData <- data.frame(algorithm = "DANN-ACO", evaluation = dannacoEvaluationsSpecial)
testData <- rbind(testData, data.frame(algorithm = "NEAT", evaluation = neatEvaluationsSpecial))


wilcox.test(evaluation~algorithm, data = testData, exact = TRUE, detailed = TRUE, alternative = "t")



evaluations <- c(754.0,493.0,1044.0,1363.0,319.0,2320.0,1276.0,1044.0,1160.0,609.0,6003.0,870.0,1044.0,957.0,377.0,899.0,4727.0,2871.0,1276.0,551.0,23055.0,1624.0,1508.0,725.0,45733.0,2407.0,4901.0,1334.0,638.0,12586.0,8874.0,348.0,3480.0,812.0,928.0,406.0,783.0,232.0,1653.0,10440.0,1508.0,2146.0,6003.0,5481.0,435.0,2204.0,5075.0,1218.0,696.0,2581.0,754.0,638.0,14181.0,1856.0,406.0,5394.0,50025.0,5104.0,870.0,725.0,1624.0,5191.0,1218.0,1247.0,3480.0,638.0,10295.0,1392.0,1508.0,841.0,493.0,870.0,3944.0,2291.0,3248.0,2262.0,8700.0,9077.0,2668.0,783.0,1479.0,899.0,2987.0,783.0,26970.0,1914.0,14616.0,2030.0,5104.0,4785.0,1247.0,1653.0,667.0,1450.0,1653.0,406.0,4756.0,522.0,870.0,2262.0,3074.0,3857.0,3973.0,7772.0,957.0,1073.0,2001.0,1363.0,3335.0,348.0,8497.0,10382.0,12760.0,899.0,1015.0,290.0,1624.0,1508.0,2059.0,8932.0,7598.0,1305.0,435.0,2668.0,725.0,14964.0,2204.0,435.0,2494.0,6409.0,667.0,841.0,3538.0,696.0,2001.0,1218.0,841.0,2523.0,580.0,754.0,1334.0,7772.0,841.0,377.0,1392.0,783.0,986.0,1363.0,899.0,3364.0,2610.0,13166.0,1218.0,696.0,319.0,638.0,2117.0,493.0,6554.0,1537.0,1682.0,348.0,667.0,3277.0,5481.0,522.0,2523.0,50025.0,7105.0,348.0,580.0,1682.0,1392.0,1102.0,319.0,2233.0,4988.0,1943.0,1334.0,2668.0,2552.0,667.0,11745.0,928.0,348.0,841.0,522.0,1595.0,348.0,1015.0,754.0,2871.0,1624.0,551.0,3422.0,812.0,493.0,870.0,50025.0,725.0)
neurons <- c(1.0,1.0,1.0,1.0,1.0,4.0,1.0,1.0,3.0,1.0,5.0,1.0,4.0,1.0,1.0,2.0,5.0,5.0,2.0,1.0,19.0,1.0,2.0,3.0,32.0,5.0,9.0,2.0,2.0,18.0,13.0,1.0,6.0,2.0,2.0,1.0,2.0,1.0,3.0,10.0,3.0,3.0,6.0,10.0,1.0,1.0,5.0,2.0,3.0,6.0,3.0,1.0,11.0,3.0,1.0,8.0,27.0,4.0,2.0,1.0,2.0,9.0,2.0,4.0,7.0,1.0,14.0,2.0,1.0,3.0,1.0,1.0,5.0,3.0,4.0,2.0,8.0,12.0,4.0,2.0,2.0,1.0,5.0,2.0,25.0,3.0,13.0,2.0,7.0,8.0,3.0,2.0,1.0,2.0,3.0,1.0,6.0,1.0,4.0,2.0,2.0,5.0,3.0,6.0,1.0,3.0,3.0,3.0,9.0,1.0,9.0,10.0,15.0,2.0,2.0,2.0,1.0,3.0,4.0,10.0,6.0,1.0,1.0,5.0,1.0,17.0,1.0,2.0,4.0,10.0,2.0,1.0,7.0,3.0,3.0,1.0,2.0,5.0,1.0,1.0,3.0,10.0,1.0,1.0,2.0,3.0,1.0,2.0,2.0,4.0,4.0,13.0,2.0,1.0,1.0,1.0,2.0,1.0,11.0,2.0,2.0,1.0,1.0,7.0,4.0,1.0,5.0,36.0,7.0,1.0,2.0,2.0,3.0,2.0,1.0,2.0,5.0,4.0,1.0,5.0,3.0,2.0,15.0,1.0,1.0,2.0,1.0,2.0,1.0,2.0,1.0,6.0,4.0,1.0,5.0,1.0,1.0,3.0,31.0,2.0)
connections <- c(6.0,6.0,7.0,8.0,7.0,22.0,7.0,7.0,10.0,6.0,23.0,6.0,21.0,7.0,6.0,9.0,20.0,21.0,10.0,7.0,113.0,8.0,11.0,10.0,266.0,21.0,43.0,11.0,8.0,88.0,70.0,6.0,25.0,8.0,8.0,6.0,8.0,6.0,11.0,52.0,14.0,11.0,26.0,40.0,6.0,7.0,29.0,10.0,9.0,24.0,11.0,7.0,53.0,15.0,7.0,51.0,193.0,13.0,9.0,7.0,11.0,38.0,10.0,16.0,23.0,6.0,70.0,9.0,6.0,10.0,7.0,6.0,25.0,12.0,23.0,9.0,41.0,58.0,21.0,11.0,9.0,6.0,23.0,9.0,144.0,12.0,58.0,8.0,32.0,37.0,14.0,11.0,7.0,10.0,15.0,8.0,30.0,8.0,15.0,8.0,9.0,24.0,14.0,36.0,6.0,12.0,13.0,10.0,42.0,6.0,30.0,50.0,78.0,10.0,8.0,8.0,7.0,13.0,20.0,49.0,35.0,7.0,7.0,18.0,6.0,96.0,7.0,7.0,20.0,49.0,8.0,8.0,34.0,13.0,14.0,7.0,8.0,26.0,6.0,6.0,17.0,62.0,7.0,7.0,9.0,12.0,7.0,9.0,11.0,15.0,16.0,69.0,8.0,6.0,6.0,9.0,9.0,6.0,59.0,8.0,10.0,7.0,8.0,28.0,24.0,9.0,19.0,243.0,39.0,7.0,8.0,11.0,11.0,9.0,6.0,8.0,25.0,19.0,7.0,18.0,13.0,9.0,65.0,7.0,6.0,10.0,7.0,9.0,7.0,10.0,7.0,25.0,21.0,7.0,25.0,7.0,7.0,11.0,213.0,8.0)
success <- c(1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,0.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,0.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,0.0,1.0)

result <- data.frame(evaluations, neurons, connections, success)

result <- result %>%
  filter(success == 1)

describeBy(result$connections)


evaluationsIsolation <- c(110.0,128.0,380.0,1226.0,299.0,1028.0,6203.0,1667.0,299.0,200.0,506.0,1037.0,155.0,281.0,1568.0,245.0,128.0,110.0,506.0,272.0,569.0,200.0,9893.0,128.0,317.0,209.0,425.0,3440.0,173.0,146.0,4628.0,164.0,173.0,290.0,110.0,227.0,191.0,92.0,4988.0,209.0,236.0,875.0,803.0,164.0,2675.0,119.0,776.0,101.0,722.0,722.0,524.0,119.0,191.0,3521.0,155.0,317.0,119.0,155.0,623.0,101.0,83.0,614.0,164.0,56.0,272.0,146.0,227.0,227.0,551.0,668.0,542.0,119.0,542.0,731.0,2405.0,605.0,497.0,236.0,236.0,263.0,191.0,119.0,281.0,83.0,1082.0,5474.0,308.0,425.0,272.0,137.0,677.0,2477.0,227.0,164.0,362.0,308.0,614.0,173.0,488.0,1244.0,272.0,1613.0,1883.0,488.0,227.0,92.0,137.0,92.0,245.0,479.0,83.0,254.0,398.0,2441.0,200.0,2720.0,218.0,263.0,10217.0,155.0,317.0,92.0,137.0,443.0,245.0,137.0,308.0,182.0,146.0,29.0,281.0,101.0,6653.0,542.0,155.0,65.0,200.0,119.0,983.0,56.0,1586.0,821.0,605.0,668.0,524.0,326.0,173.0,452.0,929.0,110.0,785.0,506.0,416.0,956.0,74.0,488.0,245.0,911.0,110.0,164.0,389.0,110.0,7877.0,596.0,2756.0,200.0,695.0,263.0,12206.0,128.0,6824.0,245.0,389.0,13142.0,56.0,173.0,263.0,209.0,209.0,299.0,2090.0,641.0,362.0,137.0,749.0,5159.0,146.0,3602.0,5825.0,479.0,263.0,101.0,299.0,1181.0,146.0,191.0,1199.0,596.0,4187.0,173.0)
evaluations <- c(434.0,515.0,677.0,308.0,578.0,1334.0,659.0,245.0,452.0,605.0,344.0,470.0,380.0,533.0,587.0,110.0,1172.0,668.0,335.0,443.0,236.0,191.0,614.0,263.0,893.0,380.0,218.0,362.0,713.0,686.0,227.0,560.0,677.0,380.0,533.0,326.0,281.0,1010.0,416.0,272.0,452.0,515.0,4943.0,452.0,1073.0,362.0,479.0,389.0,506.0,281.0,497.0,911.0,632.0,614.0,317.0,380.0,1559.0,398.0,290.0,317.0,236.0,686.0,551.0,1217.0,632.0,209.0,1514.0,191.0,398.0,776.0,1046.0,461.0,695.0,200.0,461.0,857.0,416.0,335.0,461.0,308.0,650.0,1793.0,416.0,443.0,533.0,605.0,371.0,551.0,299.0,2720.0,254.0,11.0,434.0,236.0,245.0,326.0,632.0,218.0,452.0,1910.0,1118.0,803.0,101.0,551.0,1136.0,281.0,398.0,731.0,461.0,668.0,578.0,146.0,218.0,182.0,290.0,200.0,263.0,1469.0,398.0,785.0,776.0,272.0,533.0,173.0,443.0,515.0,560.0,191.0,317.0,128.0,65.0,326.0,605.0,353.0,317.0,677.0,902.0,542.0,191.0,308.0,452.0,218.0,569.0,245.0,1424.0,767.0,308.0,848.0,317.0,353.0,344.0,434.0,632.0,1073.0,677.0,632.0,542.0,1253.0,470.0,74.0,173.0,587.0,794.0,452.0,308.0,110.0,245.0,425.0,344.0,335.0,1640.0,335.0,704.0,740.0,173.0,290.0,605.0,191.0,470.0,218.0,218.0,92.0,992.0,560.0,398.0,344.0,1163.0,119.0,1208.0,110.0,299.0,578.0,893.0,407.0,128.0,911.0,1289.0,344.0,128.0,191.0)
neuronsIsolation <- c(0.0,0.0,2.0,1.0,1.0,0.0,1.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,1.0,0.0,1.0,1.0,1.0,2.0,0.0,3.0,0.0,0.0,1.0,1.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,1.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,1.0,0.0,1.0,0.0,1.0,0.0,1.0,2.0,0.0,0.0,1.0,1.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,1.0,0.0,0.0,0.0,0.0,1.0,1.0,1.0,0.0,0.0,0.0,0.0,1.0,0.0,0.0,0.0,0.0,0.0,1.0,0.0,0.0,0.0,0.0,1.0,0.0,1.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,2.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,1.0,0.0,0.0,1.0,0.0,0.0,0.0,1.0,0.0,0.0,0.0,0.0,1.0,0.0,0.0,0.0,0.0,1.0,0.0,1.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,1.0,0.0,0.0,0.0,0.0,1.0,0.0,0.0,0.0,2.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,1.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,2.0,0.0,1.0,0.0,0.0,0.0,2.0,0.0,1.0,0.0,0.0,0.0,1.0,1.0,0.0,0.0,1.0,0.0,0.0,0.0,0.0,1.0,0.0,0.0,1.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0)
neurons <- c(1.0,0.0,1.0,3.0,3.0,1.0,2.0,0.0,1.0,1.0,1.0,1.0,0.0,1.0,1.0,1.0,1.0,0.0,1.0,1.0,1.0,1.0,0.0,0.0,1.0,0.0,1.0,0.0,1.0,2.0,0.0,2.0,0.0,0.0,0.0,0.0,0.0,1.0,1.0,1.0,2.0,1.0,2.0,0.0,0.0,2.0,1.0,1.0,1.0,0.0,2.0,1.0,0.0,1.0,0.0,1.0,1.0,1.0,1.0,1.0,0.0,0.0,1.0,1.0,1.0,0.0,1.0,3.0,1.0,0.0,1.0,1.0,3.0,1.0,1.0,1.0,0.0,1.0,2.0,0.0,1.0,2.0,1.0,1.0,1.0,1.0,1.0,1.0,0.0,2.0,1.0,0.0,1.0,1.0,0.0,0.0,1.0,0.0,1.0,2.0,1.0,0.0,0.0,0.0,1.0,1.0,1.0,1.0,0.0,1.0,2.0,0.0,1.0,1.0,1.0,1.0,0.0,1.0,0.0,1.0,2.0,1.0,0.0,1.0,2.0,1.0,0.0,1.0,0.0,0.0,0.0,0.0,2.0,1.0,2.0,3.0,1.0,1.0,2.0,0.0,0.0,1.0,1.0,0.0,1.0,1.0,0.0,1.0,0.0,1.0,1.0,1.0,2.0,2.0,1.0,1.0,2.0,2.0,0.0,0.0,1.0,1.0,1.0,0.0,1.0,0.0,0.0,0.0,1.0,1.0,1.0,1.0,1.0,1.0,0.0,1.0,1.0,0.0,2.0,2.0,0.0,1.0,0.0,0.0,1.0,1.0,0.0,1.0,1.0,1.0,1.0,0.0,0.0,1.0,1.0,1.0,2.0,0.0,1.0,0.0)
connectionsIsolation <- c(3.0,3.0,8.0,6.0,4.0,3.0,4.0,4.0,4.0,4.0,6.0,3.0,4.0,2.0,3.0,4.0,3.0,3.0,5.0,5.0,5.0,3.0,9.0,3.0,4.0,6.0,3.0,2.0,2.0,2.0,3.0,5.0,3.0,5.0,4.0,5.0,3.0,4.0,5.0,3.0,3.0,4.0,6.0,2.0,5.0,4.0,3.0,4.0,7.0,9.0,2.0,3.0,4.0,7.0,2.0,2.0,5.0,3.0,3.0,5.0,3.0,4.0,2.0,4.0,4.0,3.0,4.0,5.0,2.0,3.0,4.0,2.0,6.0,4.0,6.0,2.0,2.0,4.0,4.0,5.0,3.0,5.0,4.0,4.0,4.0,7.0,2.0,3.0,4.0,3.0,7.0,3.0,6.0,3.0,4.0,2.0,4.0,4.0,2.0,5.0,2.0,8.0,3.0,4.0,3.0,3.0,2.0,4.0,2.0,4.0,4.0,6.0,4.0,2.0,7.0,4.0,3.0,3.0,6.0,3.0,3.0,3.0,3.0,5.0,3.0,2.0,4.0,4.0,6.0,4.0,6.0,3.0,4.0,4.0,3.0,3.0,3.0,3.0,6.0,3.0,5.0,4.0,4.0,4.0,5.0,4.0,3.0,6.0,3.0,4.0,2.0,2.0,3.0,4.0,2.0,3.0,5.0,6.0,2.0,5.0,3.0,2.0,4.0,4.0,8.0,2.0,3.0,3.0,6.0,3.0,9.0,2.0,4.0,5.0,2.0,5.0,6.0,6.0,4.0,3.0,6.0,4.0,3.0,3.0,2.0,6.0,4.0,4.0,8.0,6.0,3.0,4.0,3.0,2.0,3.0,2.0,2.0,3.0,4.0,4.0)
connections <- c(8.0,6.0,9.0,10.0,10.0,9.0,12.0,6.0,7.0,10.0,7.0,10.0,5.0,8.0,8.0,8.0,8.0,5.0,8.0,7.0,7.0,8.0,6.0,5.0,8.0,6.0,8.0,6.0,8.0,8.0,5.0,8.0,6.0,5.0,6.0,5.0,6.0,8.0,8.0,6.0,8.0,7.0,11.0,6.0,6.0,9.0,7.0,8.0,8.0,6.0,8.0,7.0,6.0,6.0,5.0,6.0,9.0,7.0,8.0,8.0,5.0,6.0,8.0,10.0,9.0,6.0,8.0,9.0,8.0,5.0,7.0,6.0,10.0,7.0,6.0,9.0,6.0,7.0,8.0,5.0,9.0,11.0,8.0,8.0,7.0,9.0,7.0,8.0,5.0,12.0,6.0,5.0,10.0,8.0,6.0,5.0,11.0,5.0,9.0,12.0,7.0,6.0,5.0,5.0,9.0,8.0,7.0,7.0,6.0,8.0,10.0,5.0,7.0,8.0,7.0,7.0,6.0,8.0,6.0,6.0,9.0,7.0,5.0,9.0,9.0,10.0,5.0,7.0,5.0,6.0,6.0,6.0,9.0,7.0,9.0,11.0,8.0,6.0,9.0,5.0,6.0,9.0,7.0,5.0,7.0,8.0,5.0,8.0,6.0,10.0,7.0,8.0,9.0,9.0,10.0,7.0,10.0,10.0,5.0,5.0,8.0,6.0,7.0,5.0,6.0,5.0,6.0,5.0,8.0,6.0,9.0,8.0,7.0,8.0,6.0,7.0,8.0,5.0,8.0,8.0,5.0,7.0,6.0,6.0,9.0,7.0,6.0,7.0,9.0,6.0,7.0,5.0,5.0,7.0,7.0,6.0,14.0,5.0,7.0,5.0)

result <- data.frame(method = "Neuronisolation", property = "Evaluationen", value = evaluationsIsolation)
result <- rbind(result, data.frame(method = "Neuronisolation", property = "Verdeckte Neurone", value = neuronsIsolation))
result <- rbind(result, data.frame(method = "Neuronisolation", property = "Verbindungen", value = connectionsIsolation))
result <- rbind(result, data.frame(method = "Keine Neuronisolation", property = "Evaluationen", value = evaluations))
result <- rbind(result, data.frame(method = "Keine Neuronisolation", property = "Verdeckte Neurone", value = neurons))
result <- rbind(result, data.frame(method = "Keine Neuronisolation", property = "Verbindungen", value = connections))

describeBy(evaluationsIsolation)
describeBy(evaluations)

ggplot(result, aes(x = method, y = value, fill = method)) +
  facet_wrap(.~property, scales = "free") +
  geom_violin(alpha=0.75) +
  geom_boxplot(width=0.25, outlier.alpha = 0) +
  xlab("") +
  ylab("Wert") +
  scale_y_continuous(breaks = scales::pretty_breaks()) +
  theme(axis.title.x = element_text(size = 18, family = "LM Roman 10"),
        axis.title.y = element_blank(),
        panel.border = element_rect(colour = "black", fill = NA, size = 1),
        plot.title = element_text(family = "LM Roman 10", hjust = 0.5, size = 24, margin = margin(0, 0, 15, 0)),
        axis.text = element_text(size = 18, family = "LM Roman 10"),
        axis.text.x = element_blank(),
        axis.ticks.x = element_blank(),
        legend.text = element_text(size = 18, family = "LM Roman 10"),
        legend.title = element_blank(),
        legend.position = "right",
        strip.text.x = element_text(size = 18, family = "LM Roman 10"),
        strip.background = element_blank())





# dannacoResult <- extractIterationData("C:/Users/emaeu/IdeaProjects/ParticleEnvironment/temp/config_455.txt", "CONFIG_455")
# neatResult <- extractIterationData("C:/Users/emaeu/IdeaProjects/ParticleEnvironment/temp/config_918.txt", "CONFIG_918")

# dannacoResult <- extractIterationData("C:/Users/emaeu/IdeaProjects/ParticleEnvironment/temp/xor_general_without_recurrent.txt", "Standard")
# ablationResult <- extractIterationData("C:/Users/emaeu/IdeaProjects/ParticleEnvironment/temp/ablation_xor_no pheromone_sharing.txt", "Ablation")

run1 <- extractIterationData("C:/Users/emaeu/IdeaProjects/ParticleEnvironment/temp/execution_2021-08-12_08-38-34-984.txt", "Keine Übernahme")
run2 <- extractIterationData("C:/Users/emaeu/IdeaProjects/ParticleEnvironment/temp/execution_2021-08-12_08-21-59-634.txt", "Mit Übernahme")

#drawRunSummary("C:\\Users\\emaeu\\IdeaProjects\\ParticleEnvironment\\temp\\important_runs\\aco_xor_non_recurrent.txt", 5)

# compareResultsFromDifferentFiles(dannacoResult, neatResult)

# box_plot <- boxPlotEvaluations(dannacoResult, ablationResult)

trendOfWeights(run1, run2)

standard_result <- c(1.0, 1.0, 1.0, 2.0, 2.0, 2.0, 4.0, 2.0, 1.0, 1.0, 1.0, 2.0, 1.0, 1.0, 2.0, 1.0, 2.0, 1.0, 2.0, 2.0, 1.0, 3.0, 1.0, 2.0, 5.0, 2.0, 1.0, 1.0, 1.0, 2.0, 3.0, 1.0, 1.0, 4.0, 2.0, 1.0, 1.0, 1.0, 2.0, 3.0, 1.0, 1.0, 1.0, 1.0, 3.0, 3.0, 1.0, 2.0, 3.0, 1.0, 1.0, 1.0, 3.0, 3.0, 1.0, 2.0, 2.0, 1.0, 1.0, 2.0, 3.0, 1.0, 1.0, 1.0, 3.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 4.0, 1.0, 3.0, 1.0, 5.0, 1.0, 1.0, 2.0, 2.0, 3.0, 1.0, 4.0, 1.0, 1.0, 1.0, 4.0, 1.0, 1.0, 2.0, 1.0, 2.0, 5.0, 2.0, 1.0, 3.0, 1.0, 1.0, 2.0, 1.0, 2.0, 2.0, 1.0, 1.0, 1.0, 1.0, 2.0, 3.0, 4.0, 2.0, 1.0, 3.0, 2.0, 2.0, 1.0, 1.0, 1.0, 1.0, 2.0, 2.0, 1.0, 1.0, 2.0, 1.0, 1.0, 1.0, 2.0, 1.0, 1.0, 2.0, 2.0, 1.0, 2.0, 1.0, 3.0, 1.0, 2.0, 4.0, 2.0, 2.0, 1.0, 1.0, 3.0, 1.0, 1.0, 1.0, 2.0, 2.0, 3.0, 1.0, 1.0, 2.0, 2.0, 2.0, 4.0, 1.0, 1.0, 3.0, 1.0, 2.0, 2.0, 2.0, 1.0, 3.0, 1.0, 2.0, 3.0, 1.0, 1.0, 2.0, 2.0, 1.0, 1.0, 5.0, 4.0, 2.0, 1.0, 1.0, 1.0, 1.0, 1.0, 2.0, 2.0, 1.0, 1.0, 1.0, 1.0, 1.0, 3.0, 1.0, 2.0, 1.0, 1.0, 1.0, 2.0, 1.0, 1.0, 3.0)
ablation_result <- c(2.0, 1.0, 3.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 2.0, 1.0, 1.0, 1.0, 1.0, 2.0, 1.0, 1.0, 1.0, 1.0, 2.0, 2.0, 2.0, 1.0, 2.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 2.0, 1.0, 1.0, 1.0, 1.0, 2.0, 1.0, 1.0, 2.0, 2.0, 1.0, 1.0, 2.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 2.0, 2.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 2.0, 1.0, 2.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 2.0, 1.0, 2.0, 1.0, 1.0, 1.0, 1.0, 2.0, 1.0, 2.0, 1.0, 2.0, 1.0, 2.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 2.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 2.0, 1.0, 1.0, 1.0, 1.0, 1.0, 2.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 2.0, 1.0, 1.0, 1.0, 1.0, 2.0, 1.0, 1.0, 2.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 2.0, 1.0, 1.0, 1.0, 1.0, 2.0, 1.0, 1.0, 2.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 2.0, 1.0, 1.0, 1.0, 2.0, 1.0)

bar_plot <- drawQuantities(list(ablation_result, standard_result), list("Ablation", "Standard"))

ggarrange(box_plot, bar_plot, common.legend = TRUE, legend = "right")







df <- data.frame(value = NULL)

files <- c("C:/Users/emaeu/Desktop/deviations.txt")
# "C:/Users/emaeu/Desktop/modification_rate.txt"
# "C:/Users/emaeu/Desktop/add_connection_rate.txt",
# "C:/Users/emaeu/Desktop/remove_connection_rate.txt",
# "C:/Users/emaeu/Desktop/split_connection_rate.txt",
# "C:/Users/emaeu/Desktop/split_instead_of_remove_rate.txt")

names <- c("Teilungsentscheidung")
# "Modifikationsrate"
# "Add connection rate",
# "Remove connection rate",
# "Split connection rate",
# "Split/ Remove rate")


for (i in seq_along(files)) {
  name <- names[i]
  con <- file(files[i], "r")

  while (TRUE) {
    line <- readLines(con, n = 1)
    if (length(line) == 0) {
      break
    }

    df <- rbind(df, list(value = as.numeric(line)))
  }

  close(con)
}

# filtered_data <- df %>%
#   group_by(name) %>%
#   summarise(median = median(value))

segment_data <- data.frame(x = c(0.5),
                           y = c(1.5158),
                           xend = c(1.5),
                           yend = c(1.5158))

ggplot(df, aes(x = 1, y = value)) +
  geom_violin(color="grey", alpha=0.5, width = 1.2) +
  geom_segment(linetype="dashed", data=segment_data, aes(x=x,y=y,yend=yend,xend=xend),inherit.aes=FALSE) +
  geom_boxplot(width=0.1, outlier.alpha = 0) +
  xlab(NULL) +
  ylab("Standardabweichung") +
  theme(axis.ticks.x = element_blank(),
        axis.text.x = element_blank(),
        axis.title.x = element_blank(),
        axis.title.y = element_text(size = 18, family = "LM Roman 10"),
        panel.border = element_rect(colour = "black", fill = NA, size = 1),
        plot.title = element_text(family = "LM Roman 10", hjust = 0.5, size = 24, margin = margin(0, 0, 15, 0)),
        axis.text = element_text(size = 18, family = "LM Roman 10"),
        strip.text.x = element_text(size = 18, family = "LM Roman 10"),
        strip.background = element_blank())

normal <- c(560.0,560.0,596.0,488.0,785.0,1109.0,578.0,416.0,353.0,956.0,812.0,506.0,596.0,245.0,434.0,452.0,560.0,551.0,668.0,371.0,2369.0,416.0,605.0,740.0,596.0,1415.0,965.0,938.0,389.0,857.0,659.0,623.0,452.0,1253.0,398.0,632.0,1316.0,866.0,506.0,506.0,434.0,641.0,191.0,524.0,938.0,488.0,497.0,803.0,731.0,686.0,641.0,1073.0,443.0,857.0,974.0,560.0,533.0,605.0,983.0,614.0,1028.0,281.0,461.0,1064.0,947.0,353.0,569.0,542.0,668.0,515.0,731.0,1046.0,776.0,533.0,758.0,1325.0,2216.0,1028.0,731.0,398.0,344.0,740.0,470.0,560.0,389.0,947.0,371.0,1091.0,470.0,434.0,803.0,668.0,416.0,1280.0,731.0,812.0,839.0,695.0,3107.0,1037.0,551.0,785.0,425.0,542.0,533.0,803.0,488.0,1064.0,578.0,452.0,542.0,443.0,614.0,1055.0,497.0,245.0,794.0,281.0,398.0,569.0,1028.0,533.0,1667.0,893.0,1946.0,668.0,659.0,398.0,794.0,992.0,767.0,803.0,407.0,245.0,578.0,497.0,668.0,425.0,1199.0,4160.0,1451.0,551.0,461.0,1289.0,344.0,614.0,1847.0,569.0,947.0,1055.0,416.0,443.0,704.0,731.0,425.0,659.0,677.0,272.0,920.0,1676.0,785.0,920.0,254.0,596.0,578.0,380.0,740.0,461.0,1055.0,515.0,830.0,785.0,785.0,371.0,803.0,596.0,362.0,623.0,3602.0,767.0,686.0,947.0,632.0,911.0,1127.0,560.0,1199.0,2810.0,695.0,317.0,470.0,524.0,533.0,1082.0,641.0,1676.0,965.0,524.0,398.0,776.0)
ablation <- c(2063.0,776.0,2315.0,1190.0,407.0,3008.0,902.0,1361.0,1541.0,974.0,434.0,1082.0,3836.0,1019.0,695.0,317.0,2315.0,677.0,506.0,947.0,1658.0,1253.0,2108.0,587.0,524.0,434.0,182.0,812.0,317.0,1001.0,9002.0,1730.0,758.0,551.0,1307.0,1829.0,911.0,461.0,1460.0,1424.0,443.0,1136.0,1316.0,686.0,713.0,1640.0,1658.0,533.0,911.0,5177.0,290.0,407.0,1964.0,2756.0,794.0,1352.0,2801.0,1316.0,1496.0,308.0,875.0,1127.0,1424.0,326.0,389.0,2621.0,20000.0,1433.0,2288.0,821.0,3188.0,1361.0,1532.0,1505.0,578.0,569.0,362.0,2279.0,713.0,776.0,3665.0,1388.0,389.0,1442.0,533.0,929.0,290.0,2018.0,686.0,1001.0,461.0,1091.0,614.0,911.0,542.0,272.0,524.0,2027.0,3494.0,974.0,1208.0,335.0,1280.0,1505.0,2054.0,1613.0,902.0,1235.0,578.0,12170.0,614.0,758.0,722.0,1928.0,1289.0,2297.0,1919.0,821.0,1208.0,1676.0,2063.0,551.0,479.0,1073.0,623.0,380.0,1073.0,1118.0,812.0,920.0,956.0,2243.0,317.0,839.0,1271.0,776.0,749.0,821.0,632.0,263.0,650.0,974.0,2306.0,1109.0,1298.0,1100.0,713.0,650.0,974.0,542.0,1604.0,227.0,776.0,2513.0,1199.0,1316.0,551.0,1163.0,3665.0,569.0,650.0,1541.0,1649.0,1100.0,1568.0,2639.0,425.0,1586.0,479.0,515.0,776.0,425.0,380.0,650.0,416.0,857.0,704.0,1883.0,2189.0,524.0,317.0,767.0,830.0,7301.0,884.0,1721.0,1010.0,2180.0,704.0,1451.0,1676.0,1568.0,1136.0,677.0,389.0,1235.0,1172.0,2225.0,722.0,1109.0)

data.frame(value = ablation) %>%
  t_test(value ~ 1, mu = mean(normal), detailed = TRUE)





df <- data.frame(value = NULL)

files <- c("C:/Users/emaeu/Desktop/deviations.txt")
# "C:/Users/emaeu/Desktop/modification_rate.txt"
# "C:/Users/emaeu/Desktop/add_connection_rate.txt",
# "C:/Users/emaeu/Desktop/remove_connection_rate.txt",
# "C:/Users/emaeu/Desktop/split_connection_rate.txt",
# "C:/Users/emaeu/Desktop/split_instead_of_remove_rate.txt")

names <- c("Teilungsentscheidung")
# "Modifikationsrate"
# "Add connection rate",
# "Remove connection rate",
# "Split connection rate",
# "Split/ Remove rate")


for (i in seq_along(files)) {
  name <- names[i]
  con <- file(files[i], "r")

  while (TRUE) {
    line <- readLines(con, n = 1)
    if (length(line) == 0) {
      break
    }

    df <- rbind(df, list(value = as.numeric(line)))
  }

  close(con)
}

# filtered_data <- df %>%
#   group_by(name) %>%
#   summarise(median = median(value))

segment_data <- data.frame(x = c(0.5),
                           y = c(1.5158),
                           xend = c(1.5),
                           yend = c(1.5158))

ggplot(df, aes(x = 1, y = value)) +
  geom_violin(color="grey", alpha=0.5, width = 1.2) +
  geom_segment(linetype="dashed", data=segment_data, aes(x=x,y=y,yend=yend,xend=xend),inherit.aes=FALSE) +
  geom_boxplot(width=0.1, outlier.alpha = 0) +
  xlab(NULL) +
  ylab("Standardabweichung") +
  theme(axis.ticks.x = element_blank(),
        axis.text.x = element_blank(),
        axis.title.x = element_blank(),
        axis.title.y = element_text(size = 18, family = "LM Roman 10"),
        panel.border = element_rect(colour = "black", fill = NA, size = 1),
        plot.title = element_text(family = "LM Roman 10", hjust = 0.5, size = 24, margin = margin(0, 0, 15, 0)),
        axis.text = element_text(size = 18, family = "LM Roman 10"),
        strip.text.x = element_text(size = 18, family = "LM Roman 10"),
        strip.background = element_blank())

normal <- c(560.0,560.0,596.0,488.0,785.0,1109.0,578.0,416.0,353.0,956.0,812.0,506.0,596.0,245.0,434.0,452.0,560.0,551.0,668.0,371.0,2369.0,416.0,605.0,740.0,596.0,1415.0,965.0,938.0,389.0,857.0,659.0,623.0,452.0,1253.0,398.0,632.0,1316.0,866.0,506.0,506.0,434.0,641.0,191.0,524.0,938.0,488.0,497.0,803.0,731.0,686.0,641.0,1073.0,443.0,857.0,974.0,560.0,533.0,605.0,983.0,614.0,1028.0,281.0,461.0,1064.0,947.0,353.0,569.0,542.0,668.0,515.0,731.0,1046.0,776.0,533.0,758.0,1325.0,2216.0,1028.0,731.0,398.0,344.0,740.0,470.0,560.0,389.0,947.0,371.0,1091.0,470.0,434.0,803.0,668.0,416.0,1280.0,731.0,812.0,839.0,695.0,3107.0,1037.0,551.0,785.0,425.0,542.0,533.0,803.0,488.0,1064.0,578.0,452.0,542.0,443.0,614.0,1055.0,497.0,245.0,794.0,281.0,398.0,569.0,1028.0,533.0,1667.0,893.0,1946.0,668.0,659.0,398.0,794.0,992.0,767.0,803.0,407.0,245.0,578.0,497.0,668.0,425.0,1199.0,4160.0,1451.0,551.0,461.0,1289.0,344.0,614.0,1847.0,569.0,947.0,1055.0,416.0,443.0,704.0,731.0,425.0,659.0,677.0,272.0,920.0,1676.0,785.0,920.0,254.0,596.0,578.0,380.0,740.0,461.0,1055.0,515.0,830.0,785.0,785.0,371.0,803.0,596.0,362.0,623.0,3602.0,767.0,686.0,947.0,632.0,911.0,1127.0,560.0,1199.0,2810.0,695.0,317.0,470.0,524.0,533.0,1082.0,641.0,1676.0,965.0,524.0,398.0,776.0)
ablation <- c(2063.0,776.0,2315.0,1190.0,407.0,3008.0,902.0,1361.0,1541.0,974.0,434.0,1082.0,3836.0,1019.0,695.0,317.0,2315.0,677.0,506.0,947.0,1658.0,1253.0,2108.0,587.0,524.0,434.0,182.0,812.0,317.0,1001.0,9002.0,1730.0,758.0,551.0,1307.0,1829.0,911.0,461.0,1460.0,1424.0,443.0,1136.0,1316.0,686.0,713.0,1640.0,1658.0,533.0,911.0,5177.0,290.0,407.0,1964.0,2756.0,794.0,1352.0,2801.0,1316.0,1496.0,308.0,875.0,1127.0,1424.0,326.0,389.0,2621.0,20000.0,1433.0,2288.0,821.0,3188.0,1361.0,1532.0,1505.0,578.0,569.0,362.0,2279.0,713.0,776.0,3665.0,1388.0,389.0,1442.0,533.0,929.0,290.0,2018.0,686.0,1001.0,461.0,1091.0,614.0,911.0,542.0,272.0,524.0,2027.0,3494.0,974.0,1208.0,335.0,1280.0,1505.0,2054.0,1613.0,902.0,1235.0,578.0,12170.0,614.0,758.0,722.0,1928.0,1289.0,2297.0,1919.0,821.0,1208.0,1676.0,2063.0,551.0,479.0,1073.0,623.0,380.0,1073.0,1118.0,812.0,920.0,956.0,2243.0,317.0,839.0,1271.0,776.0,749.0,821.0,632.0,263.0,650.0,974.0,2306.0,1109.0,1298.0,1100.0,713.0,650.0,974.0,542.0,1604.0,227.0,776.0,2513.0,1199.0,1316.0,551.0,1163.0,3665.0,569.0,650.0,1541.0,1649.0,1100.0,1568.0,2639.0,425.0,1586.0,479.0,515.0,776.0,425.0,380.0,650.0,416.0,857.0,704.0,1883.0,2189.0,524.0,317.0,767.0,830.0,7301.0,884.0,1721.0,1010.0,2180.0,704.0,1451.0,1676.0,1568.0,1136.0,677.0,389.0,1235.0,1172.0,2225.0,722.0,1109.0)

data.frame(value = ablation) %>%
  t_test(value ~ 1, mu = mean(normal), detailed = TRUE)

