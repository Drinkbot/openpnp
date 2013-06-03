package org.openpnp.machine.reference;
/*
     Copyright (C) 2013 Karl Lew <karl@firepick.org>

 	This file is part of OpenPnP.

	OpenPnP is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    OpenPnP is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with OpenPnP.  If not, see <http://www.gnu.org/licenses/>.

 	For more information about OpenPnP visit http://openpnp.org
*/

import org.openpnp.spi.Head;
import org.openpnp.spi.Machine;

import java.util.Iterator;

/**
 * The reference head iterator cycles through all the heads on a machine.
 * Each invocation of the iterator() method starts the cycle with a different head
 * so that "all heads get to play."
 */
public class ReferenceHeadSequencer implements Iterable<Head> {
    private Machine machine;
    int position;

    public ReferenceHeadSequencer(Machine machine) {
        this.machine = machine;
    }

    @Override
    public Iterator<Head> iterator() {
        Iterator<Head> result = new ReferenceHeadIterator(machine, position);
        position = (position + 1) % machine.getHeads().size();
        return result;
    }

    private class ReferenceHeadIterator implements Iterator<Head> {
        private final Machine machine;
        int position;

        public ReferenceHeadIterator(Machine machine, int startPosition) {
            this.machine = machine;
            this.position = startPosition;
        }

        @Override
        public boolean hasNext() {
            return true;
        }

        @Override
        public Head next() {
            Head result = machine.getHeads().get(position);
            position = (position + 1) % machine.getHeads().size();
            return result;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
