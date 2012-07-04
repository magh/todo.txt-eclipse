package todo.txt_eclipse;

import java.io.Closeable;
import java.io.IOException;

public class Util {

	public static void close(final Closeable closeable) {
		if (closeable != null) {
			try {
				closeable.close();
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}
	}

}
