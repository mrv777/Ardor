/*
 * Copyright © 2020 Jelurida IP B.V.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,
 * no part of this software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */
package nxt.http.proxy;

import org.eclipse.jetty.proxy.AsyncMiddleManServlet;

import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

abstract class BufferingContentTransformer implements AsyncMiddleManServlet.ContentTransformer {

    public static final int MAX_CONFIRMABLE_CONTENT_LENGTH = 2 * 1024 * 1024;
    protected final HttpServletRequest clientRequest;
    private ByteArrayOutputStream os;

    public BufferingContentTransformer(HttpServletRequest clientRequest) {
        this.clientRequest = clientRequest;
    }

    @Override
    public void transform(ByteBuffer input, boolean finished, List<ByteBuffer> output) throws IOException {
        if (finished) {
            ByteBuffer allInput;
            //if confirmation is needed, buffer aside the input - see AsyncMiddleManServlet.ContentTransformer.transform
            if (os == null && !isConfirmationNeeded()) {
                allInput = input;
            } else {
                writeBuffer(input);
                byte[] bytes = os.toByteArray();
                if (bytes.length > MAX_CONFIRMABLE_CONTENT_LENGTH) {
                    clientRequest.setAttribute(Attr.REQUEST_NEEDS_CONFIRMATION, Boolean.FALSE);
                }
                allInput = ByteBuffer.wrap(bytes);
            }
            onContentAvailable(allInput);
            output.add(allInput);
        } else {
            writeBuffer(input);
        }
    }

    protected abstract void onContentAvailable(ByteBuffer allInput);

    protected boolean isConfirmationNeeded() {
        return Boolean.TRUE.equals(clientRequest.getAttribute(Attr.REQUEST_NEEDS_CONFIRMATION));
    }

    private void writeBuffer(ByteBuffer input) throws IOException {
        if (os == null) {
            os = new ByteArrayOutputStream();
        }
        byte[] b = new byte[input.remaining()];
        input.get(b);
        os.write(b);
    }
}
