package de.emaeuer.optimization.neat.mapping;

import com.anji.integration.Activator;
import com.anji.integration.ActivatorTranscriber;
import com.anji.integration.TranscriberException;
import com.anji.util.Properties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jgap.Chromosome;

public class ChromosomeSolutionMapper {

    private static final Logger LOG = LogManager.getLogger(ChromosomeSolutionMapper.class);

    private static ActivatorTranscriber TRANSCRIBER;

    public synchronized static void init(Properties props) {
        TRANSCRIBER = new ActivatorTranscriber();
        TRANSCRIBER.init(props);
    }

    public synchronized static ChromosomeSolutionMapping map(Chromosome chromosome, double maxFitness) {
        Activator activator;
        try {
            activator = TRANSCRIBER.newActivator(chromosome);
            return new ChromosomeSolutionMapping(chromosome, activator, maxFitness);
        } catch (TranscriberException e) {
            LOG.warn("Failed to map chromosome due to an unexpected exception", e);
            return null;
        }
    }

}
