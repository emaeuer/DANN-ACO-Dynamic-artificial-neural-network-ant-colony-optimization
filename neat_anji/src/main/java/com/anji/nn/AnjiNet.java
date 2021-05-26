/*
 * Copyright (C) 2004 Derek James and Philip Tucker
 *
 * This file is part of ANJI (Another NEAT Java Implementation).
 *
 * ANJI is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See
 * the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program; if
 * not, write to the Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 * 02111-1307 USA
 *
 * created by Philip Tucker on Jun 9, 2004
 */

package com.anji.nn;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Aggregates all pieces for a full neural network.
 *
 * @author Philip Tucker
 */
public class AnjiNet {

    /**
     * base XML tag
     */
    public final static String XML_TAG = "network";

    private List<Neuron> allNeurons;

    private List<Neuron> inNeurons;

    private List<Neuron> outNeurons;

    private Collection<CacheNeuronConnection> recurrentConns;

    private String name;

    /**
     * @param someNeurons
     * @param someInNeurons
     * @param someOutNeurons
     * @param someRecurrentConns
     * @param aName
     */
    public AnjiNet(Collection<Neuron> someNeurons, List<Neuron> someInNeurons, List<Neuron> someOutNeurons,
                   Collection<CacheNeuronConnection> someRecurrentConns, String aName) {
        init(someNeurons, someInNeurons, someOutNeurons, someRecurrentConns, aName);
    }

    /**
     * for testing only
     */
    protected AnjiNet() {
        // no-op
    }

    /**
     * @return number corresponding to cost of network activation in resources
     */
    public long cost() {
        long result = 0;

        for (Neuron n : allNeurons) {
            result += n.cost();
        }

        return result;
    }

    /**
     * @param someNeurons        all neurons
     * @param someInNeurons      input neurons (also included in someNeurons)
     * @param someOutNeurons     output neurons (also included in someNeurons)
     * @param someRecurrentConns recurrent connections
     * @param aName
     */
    protected void init(Collection<Neuron> someNeurons, List<Neuron> someInNeurons, List<Neuron> someOutNeurons,
                        Collection<CacheNeuronConnection> someRecurrentConns, String aName) {
        allNeurons = new ArrayList<>(someNeurons);

        inNeurons = someInNeurons;
        outNeurons = someOutNeurons;
        recurrentConns = someRecurrentConns;
        name = aName;
    }

    /**
     * @param idx
     * @return input neuron at position <code>idx</code>
     */
    public Neuron getInputNeuron(int idx) {
        return inNeurons.get(idx);
    }

    /**
     * @return number input neurons
     */
    public int getInputDimension() {
        return inNeurons.size();
    }

//	/**
//	 * @return <code>Collection</code> contains all <code>Neuron</code> objects
//	 */
//	public Collection getAllNeurons() {
//		return allNeurons;
//	}

    /**
     * @param idx
     * @return output neuron at position <code>idx</code>
     */
    public Neuron getOutputNeuron(int idx) {
        return outNeurons.get(idx);
    }

    /**
     * @param fromIdx
     * @param toIdx
     * @return output neurons from position <code>toIdx</code> (inclusive) to <code>fromIdx</code>
     * (exclusive)
     */
    public List<Neuron> getOutputNeurons(int fromIdx, int toIdx) {
        return outNeurons.subList(fromIdx, toIdx);
    }

    /**
     * @param fromIdx
     * @param toIdx
     * @return input neurons from position <code>toIdx</code> (inclusive) to <code>fromIdx</code>
     * (exclusive)
     */
    public List<Neuron> getInputNeurons(int fromIdx, int toIdx) {
        return inNeurons.subList(fromIdx, toIdx);
    }

    /**
     * @return number output neurons
     */
    public int getOutputDimension() {
        return outNeurons.size();
    }


    public List<Neuron> getInNeurons() {
        return inNeurons;
    }

    public List<Neuron> getOutNeurons() {
        return outNeurons;
    }

    public List<Neuron> getAllNeurons() {
        return allNeurons;
    }

    /**
     * @return <code>Collection</code> contains recurrent <code>Connection</code> objects
     */
    public Collection<CacheNeuronConnection> getRecurrentConns() {
        return recurrentConns;
    }

    /**
     * @see Object#toString()
     */
    public String toString() {
        return getName();
    }

    /**
     * @return the name.
     */
    public String getName() {
        return name;
    }

    /**
     * indicates a time step has passed
     */
    public void step() {
        // populate cache connections with values from previous step
        for (CacheNeuronConnection recurrentConn : recurrentConns) {
            recurrentConn.step();
        }

        // notify all neurons to recalculate value for current step
        for (Neuron neuron : allNeurons) {
            neuron.step();
        }
    }

    /**
     * make sure all neurons have been activated for the current cycle; this is to catch neurons
     * with no forward outputs
     */
    public void fullyActivate() {
        for (Neuron n : allNeurons) {
            n.getValue();
        }
    }

    /**
     * clear all memory in network, including neurons and recurrent connections
     */
    public void reset() {
        for (Neuron neuron : allNeurons) {
            neuron.reset();
        }

        for (CacheNeuronConnection recurrentConn : recurrentConns) {
            recurrentConn.reset();
        }
    }

    /**
     * @return <code>String</code> XML representation
     */
    public String toXml() {
        StringBuffer result = new StringBuffer();
        result.append("<").append(XML_TAG).append(">\n");
        result.append("<title>").append(getName()).append("</title>\n");
        Neuron.appendToXml(allNeurons, outNeurons, result);
        NeuronConnection.appendToXml(allNeurons, result);
        result.append("</").append(XML_TAG).append(">\n");

        return result.toString();
    }

    /**
     * @return true if network contains any recurrent connections, false otherwise
     */
    public boolean isRecurrent() {
        return !recurrentConns.isEmpty();
    }

}
