/*
 * This file is part of the PSL software.
 * Copyright 2011-2015 University of Maryland
 * Copyright 2013-2019 The Regents of the University of California
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.linqs.psl.experimental.reasoner.general;

import org.linqs.psl.config.Config;
import org.linqs.psl.reasoner.Reasoner;
import org.linqs.psl.reasoner.term.TermStore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;

/**
 * A reasoner that calls just serialzes it's terms and then returns.
 * It doesn't even wait for a response.
 */
public class DummySerialReasoner implements Reasoner {
    private static final Logger log = LoggerFactory.getLogger(DummySerialReasoner.class);

    /**
     * Prefix of property keys used by this class.
     */
    public static final String CONFIG_PREFIX = "dummyserialreasoner";

    /**
     * The path the to output file.
     */
    public static final String OUTPUT_PATH_KEY = CONFIG_PREFIX + ".executablepath";
    public static final String OUTPUT_PATH_DEFAULT =
            Paths.get(System.getProperty("java.io.tmpdir"), "psl_serial_model.txt").toString();

    private String path;

    public DummySerialReasoner() {
        super();

        path = Config.getString(OUTPUT_PATH_KEY, OUTPUT_PATH_DEFAULT);
    }

    @Override
    public void optimize(TermStore termStore) {
        if (!(termStore instanceof SerialTermStore)) {
            throw new IllegalArgumentException("Requires a SerialTermStore");
        }

        log.debug("Writing model file: " + path);
        File modelFile = new File(path);

        try (BufferedWriter modelWriter = new BufferedWriter(new FileWriter(modelFile))) {
            ((SerialTermStore)termStore).serialize(modelWriter);
        } catch (IOException ex) {
            throw new RuntimeException("Failed to write model file: " + path, ex);
        }
    }

    @Override
    public void close() {
    }
}
