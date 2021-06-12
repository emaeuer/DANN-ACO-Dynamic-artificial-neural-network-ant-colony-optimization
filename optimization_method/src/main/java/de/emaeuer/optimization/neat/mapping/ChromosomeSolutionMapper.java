package de.emaeuer.optimization.neat.mapping;

import com.anji.integration.Activator;
import com.anji.integration.ActivatorTranscriber;
import com.anji.integration.TranscriberException;
import com.anji.util.Properties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jgap.Chromosome;

public class ChromosomeSolutionMapper {

    private static Logger LOG = LogManager.getLogger(ChromosomeSolutionMapper.class);

    private final ActivatorTranscriber transcriber;

    public ChromosomeSolutionMapper(Properties props) {
        this.transcriber = new ActivatorTranscriber();
        this.transcriber.init(props);
    }

    public ChromosomeSolutionMapping map(Chromosome chromosome) {
        Activator activator;
        try {
            activator = this.transcriber.newActivator(chromosome);
            return new ChromosomeSolutionMapping(chromosome, activator);
        } catch (TranscriberException e) {
            LOG.warn("Failed to map chromosome due to an unexpected exception", e);
            return null;
        }
    }

}
