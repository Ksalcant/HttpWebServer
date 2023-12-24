/*
Simple Web Server in Java which allows you to call 
localhost:9000/ and show you the root.html webpage from the www/root.html folder
You can also do some other simple GET requests:
1) /random shows you a random picture (well random from the set defined)
2) json shows you the response as JSON for /random instead the html page
3) /file/filename shows you the raw file (not as HTML)
4) /multiply?num1=3&num2=4 multiplies the two inputs and responses with the result
5) /github?query=users/amehlhase316/repos (or other GitHub repo owners) will lead to receiving
   JSON which will for now only be printed in the console. See the todo below

The reading of the request is done "manually".
*/

package funHttpServer;

import java.io.BufferedReader;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;
import java.util.Map;
import java.util.LinkedHashMap;
import java.nio.charset.Charset;

import static java.lang.System.exit;

class WebServer {
    public static void main(String args[]) {
        WebServer server = new WebServer(9000);
    }

    /**
     * Main thread
     *
     * @param port to listen on
     */
    public WebServer(int port) {
        ServerSocket server = null;
        Socket sock = null;
        InputStream in = null;
        OutputStream out = null;

        try {
            server = new ServerSocket(port);
            while (true) {
                sock = server.accept();
                out = sock.getOutputStream();
                in = sock.getInputStream();
                byte[] response = createResponse(in);
                out.write(response);
                out.flush();
                in.close();
                out.close();
                sock.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (sock != null) {
                try {
                    server.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Used in the "/random" endpoint
     */
    private final static HashMap<String, String> _images = new HashMap<>() {
        {
            put("streets", "https://iili.io/JV1pSV.jpg");
            put("bread", "https://iili.io/Jj9MWG.jpg");
        }
    };

    private Random random = new Random();

    /**
     * Reads in socket stream and generates a response
     *
     * @param inStream HTTP input stream from socket
     * @return the byte encoded HTTP response
     */
    public byte[] createResponse(InputStream inStream) {

        byte[] response = null;
        BufferedReader in = null;

        try {

            // Read from socket's input stream. Must use an
            // InputStreamReader to bridge from streams to a reader
            in = new BufferedReader(new InputStreamReader(inStream, "UTF-8"));

            // Get header and save the request from the GET line:
            // example GET format: GET /index.html HTTP/1.1

            String request = null;

            boolean done = false;
            while (!done) {
                String line = in.readLine(); // reading the line from the bufferedReader

                System.out.println("Received: " + line);

                // find end of header("\n\n")
                if (line == null || line.equals(""))
                    done = true;
                    // parse GET format ("GET <path> HTTP/1.1")
                else if (line.startsWith("GET")) {
                    int firstSpace = line.indexOf(" ");
                    int secondSpace = line.indexOf(" ", firstSpace + 1);

                    // extract the request, basically everything after the GET up to HTTP/1.1
                    request = line.substring(firstSpace + 2, secondSpace);
                }

            }
            System.out.println("FINISHED PARSING HEADER\n");

            // Generate an appropriate response to the user
            if (request == null) {
                response = "<html>Illegal request: no GET</html>".getBytes();
            } else {
                // create output buffer
                StringBuilder builder = new StringBuilder();
                // NOTE: output from buffer is at the end

                if (request.length() == 0) {// No request so just display the default html.
                    // shows the default directory page

                    // opens the root.html file
                    String page = new String(readFileInBytes(new File("www/root.html")));
                    // performs a template replacement in the page
                    page = page.replace("${links}", buildFileList());

                    // Generate response
                    builder.append("HTTP/1.1 200 OK\n");
                    builder.append("Content-Type: text/html; charset=utf-8\n");
                    builder.append("\n");
                    builder.append(page);

                } else if (request.equalsIgnoreCase("json")) {
                    // shows the JSON of a random image and sets the header name for that image

                    // pick a index from the map
                    int index = random.nextInt(_images.size());

                    // pull out the information
                    String header = (String) _images.keySet().toArray()[index];
                    String url = _images.get(header);

                    // Generate response
                    builder.append("HTTP/1.1 200 OK\n");
                    builder.append("Content-Type: application/json; charset=utf-8\n");
                    builder.append("\n");
                    builder.append("{");
                    builder.append("\"header\":\"").append(header).append("\",");
                    builder.append("\"image\":\"").append(url).append("\"");
                    builder.append("}");

                } else if (request.equalsIgnoreCase("random")) {
                    // opens the random image page

                    // open the index.html
                    File file = new File("www/index.html");

                    // Generate response
                    builder.append("HTTP/1.1 200 OK\n");
                    builder.append("Content-Type: text/html; charset=utf-8\n");
                    builder.append("\n");
                    builder.append(new String(readFileInBytes(file)));

                } else if (request.contains("file/")) {
                    // tries to find the specified file and shows it or shows an error

                    // take the path and clean it. try to open the file
                    File file = new File(request.replace("file/", ""));

                    // Generate response
                    if (file.exists()) { // success
                        builder.append("HTTP/1.1 200 OK\n");
                        builder.append("Content-Type: text/html; charset=utf-8\n");
                        builder.append("\n");
                        builder.append("Would theoretically be a file but removed this part, you do not have to do anything with it for the assignment");
                    } else { // failure
                        builder.append("HTTP/1.1 404 Not Found\n");
                        builder.append("Content-Type: text/html; charset=utf-8\n");
                        builder.append("\n");
                        builder.append("File not found: " + file);
                    }
                } else if (request.contains("multiply?")) {
                    // This multiplies two numbers, there is NO error handling, so when
                    // wrong data is given this just crashes
                    Map<String, String> query_pairs = null;
                    // extract path parameters
                    if ((request.contains("num1")) && (request.contains("num2"))) { // This are the queries

/////////// THE SERVER DOES NOT KNOW IF THE QUERY VARIABLES HOLD VALUES OR NOT. ////////////
                        try {
                            query_pairs = splitQuery(request.replace("multiply?", ""));
                           System.out.println("Already split Query and replace request");
                        } catch (IOException io) {
                            builder.append("HTTP/1.1 200 OK\n");
                            builder.append("Content-Type: text/html; charset=utf-8\n");
                            builder.append("\n");
                            builder.append("Good choice. Please also add the queries'num1 & num2' and assign integers to each of them");


                        }
                        // extract required fields from parameters
                        try {

                            Integer num1 = Integer.parseInt(query_pairs.get("num1"));

                            Integer num2 = Integer.parseInt(query_pairs.get("num2"));
                            // do math
                            if ((-2147483646 < num1 && num1 < 2147483646) && (-2147483646 < num2 && num2 < 2147483646)) {
                                Integer result = num1 * num2;

                                // Generate response
                                builder.append("HTTP/1.1 200 OK\n");
                                builder.append("Content-Type: text/html; charset=utf-8\n");
                                builder.append("\n");
                                builder.append("Result is: " + result);
                            } else {
                                System.out.println("Values are too high");
                                builder.append("HTTP/1.1 500 Server error \n");
                                builder.append("Content-Type: text/html; charset=utf-8\n");
                                builder.append("\n");
                                builder.append(" 500: Relax, those value are too high for me still. \n");

                            }


                        } catch (Exception e) {
                            System.out.println(e);
                            builder.append("HTTP/1.1 400 Bad request! \n");
                            builder.append("Content-Type: text/html; charset=utf-8\n");
                            builder.append("\n");
                            builder.append(" 400: Wrong query, please try queries as follow: 'num1=3&num2=7' \n");


                        }
                    }else{
                        builder.append("HTTP/1.1 200 OK\n");
                        builder.append("Content-Type: text/html; charset=utf-8\n");
                        builder.append("\n");
                        builder.append("Good choice. However, you need to add two queries as follow: 'num1 & num2' and assign integers to each");

                    }
                    // TODO: Include error handling here with a correct error code and
                } else if (request.contains("github?")) {
                    // pulls the query from the request and runs it with GitHub's REST API
                    // check out https://docs.github.com/rest/reference/
                    //
                    // HINT: REST is organized by nesting topics. Figure out the biggest one first,
                    //     then drill down to what you care about
                    // "Owner's repo is named RepoName. Example: find RepoName's contributors" translates to
                    //     "/repos/OWNERNAME/REPONAME/contributors"

                    Map<String, String> query_pairs = null;

                    if (request.equals("github?")) {
                        builder.append("HTTP/1.1 400 \n");
                        builder.append("Content-Type: text/html; charset=utf-8\n");
                        builder.append("\n");
                        builder.append("'github?' is the right path. Now just add the query and press enter to see the repos.");
                    } else {
                        if (request.contains("query=users/amehlhase316/repos") == true) {

                            try {
                                query_pairs = splitQuery(request.replace("github?", ""));// here we are only sending queries

                                String json = fetchURL("https://api.github.com/" + query_pairs.get("query"));

                                try {
                                    JSONArray jsonArray = new JSONArray(json);
                                    String jsonFinal = "[\n{";
                                    for (int i = 0; i < jsonArray.length(); i++) {

                                        JSONObject jsonObject = jsonArray.getJSONObject(i);

                                        String fullName = jsonObject.getString("full_name");

                                        int id = jsonObject.getInt("id"); // is this an int?
                                        JSONObject owner = jsonObject.getJSONObject("owner");
                                        String login = owner.getString("login");

                                        jsonFinal = jsonFinal + "fullname : "+fullName + ",\n" + "id : " + id + ",\n" + "loginname : " + login + "\n";

                                    }jsonFinal = "}\n]";
// Success////////////////////////////////////////////////////////////////////////////////
                                    builder = new StringBuilder();
                                    builder.append("HTTP/1.1 200 OK\n");
                                    builder.append("Content-type: text/html; charset-utf-8\n");
                                    builder.append("\n");
                                    builder.append(jsonFinal);

                                } catch (Exception e) {       ///////////////////////FAILED//////////
                                    //System.err.println(e.getMessage());
                                    builder = new StringBuilder();
                                    builder.append("HTTP/1.1 400\n");
                                    builder.append("Content-Type: text/html; charset=utf-8\n");
                                    builder.append("\n");
                                    builder.append("Error occurred while parsion JSON reponso: " + e.getMessage());


                                }
                            } catch (UnsupportedEncodingException splitfailed) {
                                builder = new StringBuilder();
                                builder.append("HTTP/1.1 500 Server error! \n");
                                builder.append("Content-Type: text/html; charset=utf-8\n");
                                builder.append("\n");
                                builder.append(" 500: It's not your fault. Please try again\n");

                            }
                        } else {
                            // System.err.println(io.getMessage());
                            builder.append("HTTP/1.1 404 \n");
                            builder.append("Content-Type: text/html; charset=utf-8\n");
                            builder.append("\n");
                            builder.append("Your queries are misesplled, please fix them and try again...");
                        }
                    }


                }else if(request.contains("weather?")) {
                    String responseStr = null;
                    int statusCode = 200;

                    try {
                        // Extracting the arguments from the request
                        Map<String, String> query_pairs = splitQuery(request.replace("weather?", ""));

                        // Extract required fields from parameters
                        String city1 = query_pairs.get("city1");
                        String city2 = query_pairs.get("city2");

                        // Check if both or at least one city is present
                        if (city1 == null && city2 == null) {
                            statusCode = 400;
                            responseStr = "Missing 'city1' and 'city2' parameters in the request for weather.";
                        } else if (city1 == null) {
                            // Fetch weather data from OpenWeatherMap API for city2
                            String apiKey = "3a134106bbc5985fac3363a523f40c4e";
                            String url2 = "http://api.openweathermap.org/data/2.5/weather?q=" + city2 + "&appid=" + apiKey;
                            String json2 = fetchURL(url2);
                            JSONObject jsonObject2 = new JSONObject(json2);
                            String weather2 = jsonObject2.getJSONArray("weather").getJSONObject(0).getString("main");

                            responseStr = "The current weather in " + city2 + " is: " + weather2;
                        } else if (city2 == null) {
                            // Fetch weather data from OpenWeatherMap API for city1
                            String apiKey = "3a134106bbc5985fac3363a523f40c4e";
                            String url1 = "http://api.openweathermap.org/data/2.5/weather?q=" + city1 + "&appid=" + apiKey;
                            String json1 = fetchURL(url1);
                            JSONObject jsonObject1 = new JSONObject(json1);
                            String weather1 = jsonObject1.getJSONArray("weather").getJSONObject(0).getString("main");

                            responseStr = "The current weather in " + city1 + " is: " + weather1;
                        } else {
                            // Fetch weather data from OpenWeatherMap API for both cities
                            String apiKey = "3a134106bbc5985fac3363a523f40c4e";
                            String url1 = "http://api.openweathermap.org/data/2.5/weather?q=" + city1 + "&appid=" + apiKey;
                            String url2 = "http://api.openweathermap.org/data/2.5/weather?q=" + city2 + "&appid=" + apiKey;
                            String json1 = fetchURL(url1);
                            String json2 = fetchURL(url2);

                            JSONObject jsonObject1 = new JSONObject(json1);
                            JSONObject jsonObject2 = new JSONObject(json2);

                            String weather1 = jsonObject1.getJSONArray("weather").getJSONObject(0).getString("main");
                            String weather2 = jsonObject2.getJSONArray("weather").getJSONObject(0).getString("main");

                            responseStr = "The current weather in " + city1 + " is: " + weather1 + "\n" +
                            "The current weather in " + city2 + " is: " + weather2;
                        }
                    } catch (Exception e) {
                        statusCode = 400;
                        responseStr = "An error occurred while processing the request for weather."+"\n"+" You need to add 'city1=nameOfCity1&city2=nameOfCity2'";
                    }

                    // Generate response
                    builder.append("HTTP/1.1 ").append(statusCode);
                    builder.append("Content-Type: text/plain; charset=UTF-8\n");
                    builder.append("\n");
                    builder.append(responseStr);

                } else if (request.contains("palindrome?")) {
                    String responseStr = null;
                    int statusCode = 200;

                    try {
                        // Extracting the arguments from the request
                        Map<String, String> query_pairs = splitQuery(request.replace("palindrome?", ""));

                        // Extract required fields from parameters
                        String str1 = query_pairs.get("str1");
                        String str2 = query_pairs.get("str2");

                        // Check if both or at least one parameter is present
                        if (str1 == null && str2 == null) {
                            statusCode = 400;
                            responseStr = "Missing 'param1' and 'param2' parameters in the request for palindrome.";
                        } else if (str1 == null) {
                            // Perform palindrome check for param2
                            String reversedParam2 = new StringBuilder(str2).reverse().toString();
                            boolean isParam2Palindrome = str2.equalsIgnoreCase(reversedParam2);

                            responseStr = "str2 (" + str2 + ") is " + (isParam2Palindrome ? "" : "not ") + "a palindrome.";
                        } else if (str2 == null) {
                            // Perform palindrome check for param1
                            String reversedParam1 = new StringBuilder(str1).reverse().toString();
                            boolean isParam1Palindrome = str1.equalsIgnoreCase(reversedParam1);

                            responseStr = "str1 (" + str1 + ") is " + (isParam1Palindrome ? "" : "not ") + "a palindrome.";
                        } else {
                            // Perform palindrome check for both parameters
                            String reversedParam1 = new StringBuilder(str1).reverse().toString();
                            String reversedParam2 = new StringBuilder(str2).reverse().toString();

                            boolean isParam1Palindrome = str1.equalsIgnoreCase(reversedParam1);
                            boolean isParam2Palindrome = str2.equalsIgnoreCase(reversedParam2);

                            responseStr = "str1 (" + str1 + ") is " + (isParam1Palindrome ? "" : "not ") + "a palindrome.\n" +
                            "str2 (" + str2 + ") is " + (isParam2Palindrome ? "" : "not ") + "a palindrome.";
                        }
                    } catch (Exception e) {
                        statusCode = 404;
                        responseStr = "An error occurred while processing the request for palindrome.";
                    }

                    // Generate response
                    builder.append("HTTP/1.1 500 ").append(statusCode).append(" Relax, this is just a startup server\n");
                    builder.append("Content-Type: text/plain; charset=UTF-8\n");
                    builder.append("\n");
                    builder.append(responseStr);
                }

                else {
                    // if the request is not recognized at all
                    builder = new StringBuilder();
                    builder.append("HTTP/1.1 400 Bad Request\n");
                    builder.append("Content-Type: text/html; charset=utf-8\n");
                    builder.append("\n");
                    builder.append("400: Invalid path. Make sure your path is correctly spelled....");

                }

                // Output
                response = builder.toString().getBytes();
            }
        } catch (IOException e) {
            e.printStackTrace();
            response = ("<html>ERROR: " + e.getMessage() + "</html>").getBytes();
        }

        return response;
    }

    /**
     * Method to read in a query and split it up correctly
     *
     * @param query parameters on path
     * @return Map of all parameters and their specific values
     * @throws UnsupportedEncodingException If the URLs aren't encoded with UTF-8
     */
    public static Map<String, String> splitQuery(String query) throws UnsupportedEncodingException {
        Map<String, String> query_pairs = new LinkedHashMap<String, String>(); // to store the key-value pairs from the query string. The LinkedHashMap is used to maintain the order of insertion.
        // "q=hello+world%2Fme&bob=5"
        String[] pairs = query.split("&"); //The input query string is split into individual key-value pairs using the "&" character as the separator.
        // ["q=hello+world%2Fme", "bob=5"]
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            query_pairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"), URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));//It then extracts the key and value substrings from the pair using substring() method
            // and decodes them using URLDecoder.decode() with "UTF-8" as the encodin
        }
        // {{"q", "hello world/me"}, {"bob","5"}}
        return query_pairs;
    }

    /**
     * Builds an HTML file list from the www directory
     *
     * @return HTML string output of file list
     */
    public static String buildFileList() {
        ArrayList<String> filenames = new ArrayList<>();

        // Creating a File object for directory
        File directoryPath = new File("www/");
        filenames.addAll(Arrays.asList(directoryPath.list()));

        if (filenames.size() > 0) {
            StringBuilder builder = new StringBuilder();
            builder.append("<ul>\n");
            for (var filename : filenames) {
                builder.append("<li>" + filename + "</li>");
            }
            builder.append("</ul>\n");
            return builder.toString();
        } else {
            return "No files in directory";
        }
    }

    /**
     * Read bytes from a file and return them in the byte array. We read in blocks
     * of 512 bytes for efficiency.
     */
    public static byte[] readFileInBytes(File f) throws IOException {

        FileInputStream file = new FileInputStream(f);
        ByteArrayOutputStream data = new ByteArrayOutputStream(file.available());

        byte buffer[] = new byte[512];
        int numRead = file.read(buffer);
        while (numRead > 0) {
            data.write(buffer, 0, numRead);
            numRead = file.read(buffer);
        }
        file.close();

        byte[] result = data.toByteArray();
        data.close();

        return result;
    }

    /**
     * a method to make a web request. Note that this method will block execution
     * for up to 20 seconds while the request is being satisfied. Better to use a
     * non-blocking request.
     *
     * @param aUrl the String indicating the query url for the OMDb api search
     * @return the String result of the http request.
     **/
    public String fetchURL(String aUrl) {
        StringBuilder sb = new StringBuilder();
        URLConnection conn = null;
        InputStreamReader in = null;
        try {
            URL url = new URL(aUrl);
            conn = url.openConnection();
            if (conn != null)
                conn.setReadTimeout(20 * 1000); // timeout in 20 seconds
            if (conn != null && conn.getInputStream() != null) {
                in = new InputStreamReader(conn.getInputStream(), Charset.defaultCharset());
                BufferedReader br = new BufferedReader(in);
                if (br != null) {
                    int ch;
                    // read the next character until end of reader
                    while ((ch = br.read()) != -1) {
                        sb.append((char) ch);
                    }
                    br.close();
                }
            }
            in.close();
        } catch (Exception ex) {
            System.out.println("Exception in url request:" + ex.getMessage());
        }
        return sb.toString();
    }
}
