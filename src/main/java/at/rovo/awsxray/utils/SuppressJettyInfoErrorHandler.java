package at.rovo.awsxray.utils;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import javax.servlet.http.HttpServletRequest;
import org.eclipse.jetty.server.handler.ErrorHandler;

/**
 * Custom implementation of a Jetty {@link ErrorHandler} in order to suppress the <em>Powered by
 * Jetty</em> element within the error page
 */
public class SuppressJettyInfoErrorHandler extends ErrorHandler
{
  @Override
  protected void writeErrorPageBody(HttpServletRequest request, Writer writer, int code, String message, boolean showStacks)
      throws IOException {
    String uri = request.getRequestURI();

    writeErrorPageMessage(request, writer, code, message, uri);
    if (showStacks)
      writeErrorPageStacks(request, writer);
  }

  @Override
  protected void writeErrorPageStacks(HttpServletRequest request,
                                      Writer writer) throws IOException {
    Throwable th = (Throwable) request.getAttribute("javax.servlet.error.exception");
    if (th != null) {
      writer.write("<h3>Caused by:</h3><pre>");
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      pw.append(th.getLocalizedMessage());
      pw.flush();
      // sanitizes output
//      write(writer, sw.getBuffer().toString());
      // skip sanitize output
      writer.write(sw.getBuffer().toString());
      writer.write("</pre>\n");
    }
  }
}
