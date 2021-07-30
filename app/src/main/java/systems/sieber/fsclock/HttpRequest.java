package systems.sieber.fsclock;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

public class HttpRequest extends AsyncTask<Void, Void, String> {

    private File fileCert;

    private List<KeyValueItem> requestParameter;
    private List<KeyValueItem> requestHeader;
    private String requestBody = null;
    private String urlString;
    private int statusCode = 0;

    private static final String POST_PARAM_KEYVALUE_SEPARATOR = "=";
    private static final String POST_PARAM_SEPARATOR = "&";
    private static final String POST_ENCODING = "UTF-8";

    HttpRequest(String url, File fileCert) {
        this.fileCert = fileCert;
        this.urlString = url;
    }

    void setRequestParameter(List<KeyValueItem> parameter) {
        this.requestParameter = parameter;
    }
    void setRequestBody(String body) {
        this.requestBody = body;
    }
    void setRequestHeaders(List<KeyValueItem> headers) {
        this.requestHeader = headers;
    }

    private readyListener listener = null;
    public interface readyListener {
        void ready(int statusCode, String responseBody);
    }
    void setReadyListener(readyListener listener) {
        this.listener = listener;
    }

    @Override
    protected String doInBackground(Void... params) {
        return openConnection();
    }

    private String openConnection() {
        StringBuilder sb = new StringBuilder();
        if(this.requestBody != null) {
            sb.append((this.requestBody));
        } else if(this.requestParameter != null) {
            try {
                for(KeyValueItem kvi : this.requestParameter) {
                    sb.append(URLEncoder.encode(kvi.getKey(), POST_ENCODING));
                    sb.append(POST_PARAM_KEYVALUE_SEPARATOR);
                    sb.append(URLEncoder.encode(kvi.getValue(), POST_ENCODING));
                    sb.append(POST_PARAM_SEPARATOR);
                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }

        String responseText = "";
        BufferedReader reader = null;
        try {
            URL url = new URL(urlString);
            URLConnection conn = url.openConnection();

            if(conn instanceof HttpsURLConnection) {
                SSLSocketFactory ssf = getCustomSSLSocketFactory();
                if(ssf != null) {
                    ((HttpsURLConnection)conn).setSSLSocketFactory(ssf);
                }
            }

            if(this.requestHeader != null) {
                for(KeyValueItem kv : this.requestHeader) {
                    conn.setRequestProperty(kv.getKey(), kv.getValue());
                }
            }
            conn.setDoOutput(true);
            OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
            wr.write(sb.toString());
            wr.flush();

            this.statusCode = ((HttpURLConnection)conn).getResponseCode();
            if(this.statusCode == 200)
                reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            else
                reader = new BufferedReader(new InputStreamReader(((HttpURLConnection) conn).getErrorStream()));

            StringBuilder sb2 = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb2.append(line).append("\n");
            }
            responseText = sb2.toString();
            reader.close();
        } catch (Exception ex) {
            //StringWriter sw = new StringWriter();
            //PrintWriter pw = new PrintWriter(sw);
            //ex.printStackTrace(pw);
            //Log.e("HTTP", sw.toString());
            responseText = ex.getLocalizedMessage();
        } finally {
            if (reader != null) try {
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return responseText;
    }

    @Override
    protected void onPostExecute(String result) {
        if(listener != null) listener.ready(this.statusCode, result);
    }

    private SSLSocketFactory getCustomSSLSocketFactory() {
        if(fileCert == null || !fileCert.exists() || fileCert.isDirectory()) {
            return null;
        }
        try {
            // Create a KeyStore containing our trusted CAs
            String keyStoreType = KeyStore.getDefaultType();
            KeyStore keyStore = KeyStore.getInstance(keyStoreType);
            keyStore.load(null, null);

            CertificateFactory cf = CertificateFactory.getInstance("X509");
            InputStream caInput = new FileInputStream(fileCert);
            Certificate ca;
            try {
                ca = cf.generateCertificate(caInput);
                Log.i("CERT", "CA certificate [" + ((X509Certificate) ca).getSubjectDN() + "] added to truststore.");
            }
            finally
            {
                caInput.close();
            }
            keyStore.setCertificateEntry(fileCert.getName(), ca);

            // Create a TrustManager that trusts the CAs in our KeyStore
            String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
            tmf.init(keyStore);

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, tmf.getTrustManagers(), new SecureRandom());

            return sslContext.getSocketFactory();
        }
        catch (Exception e)
        {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            Log.e("CERT", sw.toString());
            return null;
        }
    }
}

class KeyValueItem {
    private String key = "";
    private String value = "";
    private boolean useValueAsInteger = false;

    public KeyValueItem(String _key, String _value) {
        key = _key;
        value = _value;
    }
    public KeyValueItem(String _key, String _value, boolean _useValueAsInteger) {
        key = _key;
        value = _value;
        useValueAsInteger = _useValueAsInteger;
    }

    public String getKey() {
        return key;
    }
    public String getValue() {
        return value;
    }
    public boolean isInteger() {
        return useValueAsInteger;
    }

    public static ArrayList<KeyValueItem> getFiltered(List<KeyValueItem> items, String filter) {
        ArrayList<KeyValueItem> filtered = new ArrayList<>();
        for(KeyValueItem c : items) {
            if(filter.equals("") || c.getValue().toUpperCase().contains(filter.toUpperCase())) {
                filtered.add(c);
            }
        }
        return filtered;
    }
}