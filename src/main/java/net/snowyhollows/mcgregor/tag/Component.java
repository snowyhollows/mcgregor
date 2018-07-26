/*
 * Copyright (c) 2009-2018 Ericsson AB, Sweden. All rights reserved.
 *
 * The Copyright to the computer program(s) herein is the property of Ericsson AB, Sweden.
 * The program(s) may be used  and/or copied with the written permission from Ericsson AB
 * or in accordance with the terms and conditions stipulated in the agreement/contract under
 * which the program(s) have been supplied.
 *
 */
package net.snowyhollows.mcgregor.tag;

import java.io.IOException;
import java.io.Writer;
import java.util.function.Consumer;

/**
 * @author efildre
 */
public interface Component {
	void render(Writer out) throws IOException;
	void setKey(String k);
	String getKey();

	default void visit(Consumer<Component> consumer) {
		consumer.accept(this);
	}
}
