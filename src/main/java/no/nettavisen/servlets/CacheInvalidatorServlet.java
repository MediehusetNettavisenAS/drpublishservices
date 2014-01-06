package no.nettavisen.servlets;


import no.nettavisen.Util.HttpPurge;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.log4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;


/**
 * Author: probal
 * Date: 12/24/13
 * Time: 4:35 PM
 */
public class CacheInvalidatorServlet extends HttpServlet {

    static Logger logger = Logger.getLogger(CacheInvalidatorServlet.class);
    private static final long serialVersionUID = 1031422249396784970L;
    private static final String nginxCacheInvalidatorUri = "http://nginx-drlib.nettavisen.no/purge/";

    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        String param_nginx_url = req.getParameter("nginx_url");
        String param_varnish_url = req.getParameter("varnish_url");
        String nginxUrl = "";
        String uriToInvalidateNginxCache = "";

        if(param_nginx_url != null && !param_nginx_url.isEmpty()) {
            nginxUrl = param_nginx_url.replace("http://","");
            nginxUrl = nginxUrl.replace("https://","");
            uriToInvalidateNginxCache = nginxCacheInvalidatorUri + nginxUrl;
        }



        HttpClient httpClient = new DefaultHttpClient();
        try {

            if(nginxUrl.length() > 0) {
                HttpGet nginxInvalidateRequest = new HttpGet(uriToInvalidateNginxCache);
                HttpResponse ngixnInvalidateResponse = httpClient.execute(nginxInvalidateRequest);
                int invalidateNginxCode = ngixnInvalidateResponse.getStatusLine().getStatusCode();
                logger.info("Nginx invalidation Status : " + invalidateNginxCode + " URL: " + uriToInvalidateNginxCache);

                HttpGet nginxRequest = new HttpGet(param_nginx_url);
                HttpResponse nginxResponse = httpClient.execute(nginxRequest);
                int refetchNginxResponseCode = nginxResponse.getStatusLine().getStatusCode();
                logger.info("Nginx refetch Status : " + refetchNginxResponseCode + " URL: " + param_nginx_url);


                if(refetchNginxResponseCode == 200 && param_varnish_url != null && !param_varnish_url.isEmpty()) {
                    HttpPurge httpPurge = new HttpPurge(param_varnish_url);
                    HttpResponse varnishResp = httpClient.execute(httpPurge);
                    int varnishCode = varnishResp.getStatusLine().getStatusCode();
                    logger.info("Varnish PURGE Status : " + varnishCode + " URL: " + param_varnish_url);
                }
            } else {
                if(param_varnish_url != null && !param_varnish_url.isEmpty()) {
                    HttpPurge httpPurge = new HttpPurge(param_varnish_url);
                    HttpResponse staleVarnishResponse = httpClient.execute(httpPurge);
                    int staleVarnishCode = staleVarnishResponse.getStatusLine().getStatusCode();
                    logger.info("nginx_url empty and Varnish PURGE Status : " + staleVarnishCode + " URL: " + param_varnish_url);
                }
            }

        } catch (Exception ex) {
            logger.error("Exception occured for nginx_url = " + param_nginx_url + " varnish_url = " + param_varnish_url, ex);
        }

    }
}
