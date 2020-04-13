package org.coepi.api.v4

import java.nio.ByteBuffer

// TODO: Add Report attributes rvk, j1, j2, memo, cek and serialization code
class TCNReport {

    companion object {
        fun from(data: ByteArray) {
            val byteBuffer = ByteBuffer.wrap(data)
            // TODO: Do stuff with the ByteBuffer and validate signature
        }
    }
}