package net.foulest.sec;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import serpapi.SerpApiSearch;
import serpapi.SerpApiSearchException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SEC {

    public static String API_KEY = "";
    public static List<String> results = new ArrayList<>();
//    public static boolean googleBlocking = false;
//    public static boolean yandexBlocking = false;
//    public static boolean braveBlocking = false;
    // TODO: Add HaveIBeenPwned into the search

    public static void main(String[] args) {
        Scanner scnr = new Scanner(System.in);

        System.out.println();
        System.out.println("SEC: Search Engine Crawler");
        System.out.println("by Foulest");

        // Sets the user's SerpApi private key.
        System.out.println();
        System.out.print("Enter your SerpApi private key: ");
        API_KEY = scnr.nextLine();

        // Sets the term to search for.
        System.out.println();
        System.out.print("Enter term to search: ");
        String searchTerm = scnr.nextLine();

        // Searches for the search term.
        System.out.println("Searching for: " + searchTerm);
        searchTerm = searchTerm.replace(" ", "+");

        // Searches all search engines for home page results, pasting websites, and leaking forums.
        // Any term searched consumes 20 searches in the SerpApi dashboard.
        // Free users only get 100 searches per month, so a premium plan is recommended.
        // The Yahoo searches might not be necessary. This is all experimental.
        searchForTerm(searchTerm, "google", Arrays.asList("", "pastebin.com", "controlc.com",
                "paste-bin.xyz", "paste.ee", "paste2.org", "pasteio.com", "rentry.co", "textbin.net", "write.as"));
        searchForTerm(searchTerm, "yahoo", List.of("", "pastebin.com", "controlc.com",
                "paste-bin.xyz", "paste.ee", "paste2.org", "pasteio.com", "rentry.co", "textbin.net", "write.as"));

        // Searches Brave for search results.
        searchForTermOld(searchTerm, "brave", List.of(""));

        // Searches GitHub Gist for search results.
        searchForTermOld(searchTerm, "github-gist", List.of(""));

        // Searches BoardReader for search results.
        searchForTermOld(searchTerm, "boardreader", List.of(""));

        // Searches Pastebin for results via PSBDMP.
        searchPastebin(searchTerm);

        // Removes duplicates from results.
        List<String> trimmedResults = new ArrayList<>(results.stream().distinct().toList());
        Collections.sort(trimmedResults);

        // Prints the results found.
        if (trimmedResults.isEmpty()) {
            System.out.println();
            System.out.println("No results found.");
        } else {
            System.out.println();
            System.out.println("Results:");
            trimmedResults.forEach(System.out::println);
        }
    }

    // Searches for a term using SerpApi.
    // View more info: https://serpapi.com
    public static void searchForTerm(String searchTerm, String searchEngine, List<String> websites) {
        for (String website : websites) {
            Map<String, String> parameter = new HashMap<>();
            parameter.put("api_key", API_KEY);
            parameter.put("num", "100");
            parameter.put("engine", searchEngine);
            parameter.put("results", "html");

            // Defines the search query depending on our search engine.
            String websiteCheck = (website.equals("") ? "" : ("site:" + website + " "));
            String query = websiteCheck + "\"" + searchTerm + "\"";

            if ("yahoo".equals(searchEngine)) {
                parameter.put("p", query);
            } else {
                parameter.put("q", query);
            }

            SerpApiSearch search = new SerpApiSearch(parameter, searchEngine);

            // Searches for the search term using SerpApi.
            try {
                System.out.println("GET -> " + query + " (" + searchEngine + ")");
                JsonObject json = search.getJson();
                JsonArray data = json.getAsJsonArray("organic_results");

                if (data == null) {
                    continue;
                }

                for (int i = 0; i < data.size(); i++) {
                    String link = data.get(i).getAsJsonObject().get("link").getAsString();
                    link = link.replace("//www.", "//");
                    link = link.replaceAll("/$", "");

                    System.out.println("FOUND -> " + link);
                    results.add(link);
                }
            } catch (SerpApiSearchException ex) {
                ex.printStackTrace();
            }
        }
    }

    public static void searchForTermOld(String searchTerm, String searchEngine, List<String> websites) {
        for (String website : websites) {
            // Defines the search query depending on our search engine.
            String websiteCheck = website.equals("") ? "%22" : ("site%3A" + website + "+%22");

            String query = switch (searchEngine) {
                case "brave" -> "https://search.brave.com/search?q=" + websiteCheck + searchTerm + "%22";
                case "github-gist" -> "https://gist.github.com/search?q=" + websiteCheck + searchTerm + "%22";
                case "boardreader" -> "https://boardreader.com/s/%2522"
                        + searchTerm.replace(" ", "%2520").replace("+", "%2520")
                        + "%2522.html;language=English";
                default -> "";
            };

            // Grabs the website's source code.
            System.out.println("GET -> " + query);
            String websiteSrc = getWebsiteSrc(query/*, searchEngine*/);

            // Returns if the search engine is blocking our connection.
            if (websiteSrc.equals("Blocked")) {
                System.out.println("BLOCKED -> " + query);
                return;
            }

            // Returns if search engine results could not be found.
            if (websiteSrc.equals("Not found")) {
                System.out.println("NOT FOUND -> " + query);
                break;
            }

            // Parses links and adds findings to the results.
            for (String result : parseLinks(website, websiteSrc)) {
                System.out.println("FOUND -> " + result);
                results.add(result);
            }
        }
    }

//    public static void searchForTermOld(String searchTerm, String searchEngine, List<String> websites) {
//        for (String website : websites) {
//            // Cancels current & future searches if the search engine blocked our connection.
//            if ((searchEngine.equals("google") && googleBlocking)
//                    || (searchEngine.equals("yandex") && yandexBlocking)
//                    || (searchEngine.equals("brave") && braveBlocking)) {
//                return;
//            }
//
//            // Defines the search query depending on our search engine.
//            String websiteCheck = website.equals("") ? "%22" : ("site%3A" + website + "+%22");
//            String query = switch (searchEngine) {
//                case "yandex" -> "https://yandex.com/search/?text="
//                        + websiteCheck + searchTerm + "%22";
//                case "brave" -> "https://search.brave.com/search?q="
//                        + websiteCheck + searchTerm + "%22";
//                case "github-gist" -> "https://gist.github.com/search?q="
//                        + websiteCheck + searchTerm + "%22";
//                case "boardreader" -> "https://boardreader.com/s/%2522"
//                        + searchTerm.replace(" ", "%2520").replace("+", "%2520")
//                        + "%2522.html;language=English";
//                default -> "https://google.com/search?q="
//                        + websiteCheck + searchTerm + "%22&num=100";
//            };
//
//            // Grabs the website's source code.
//            System.out.println("GET -> " + query);
//            String websiteSrc = getWebsiteSrc(query, searchEngine);
//
//            // Checks for Yandex's captcha and returns if present.
//            if (searchEngine.equals("yandex") && websiteSrc.contains("CheckboxCaptcha")) {
//                yandexBlocking = true;
//                websiteSrc = "Blocked";
//            }
//
//            // Returns if the search engine is blocking our connection.
//            if (websiteSrc.equals("Blocked")) {
//                System.out.println("BLOCKED -> " + query);
//                return;
//            }
//
//            // Returns if search engine results could not be found.
//            if (websiteSrc.equals("Not found")) {
//                System.out.println("NOT FOUND -> " + query);
//                sleep(4000);
//                break;
//            }
//
//            // Parses links and adds findings to the results.
//            for (String result : parseLinks(website, websiteSrc)) {
//                System.out.println("FOUND -> " + result);
//                results.add(result);
//            }
//
//            sleep(4000);
//        }
//    }

    public static void searchPastebin(String searchTerm) {
        searchTerm = searchTerm.replace("+", "%20");
        String query = "https://psbdmp.ws/api/search/" + searchTerm;

        // Grabs PSBDMP results for the search term.
        System.out.println("GET -> " + query);
        String websiteSrc = getWebsiteSrc(query/*, "none"*/);

        // Returns if PSBDMP is blocking our connection.
        if (websiteSrc.equals("Blocked")) {
            System.out.println("BLOCKED -> " + query);
            return;
        }

        // Returns if Pastebin records aren't available.
        if (websiteSrc.equals("Not found")) {
            System.out.println("NOT FOUND -> " + query);
            return;
        }

        // Defines important variables in the JSON results.
        JsonObject json = new Gson().fromJson(websiteSrc, JsonObject.class);
        int count = json.get("count").getAsInt();
        JsonArray data = json.getAsJsonArray("data");

        // Returns if no Pastebin results are found.
        if (count == 0) {
            return;
        }

        // Prints a record for each result found.
        for (int i = 0; i < data.size(); i++) {
            String id = data.get(i).getAsJsonObject().get("id").getAsString();
            String tags = data.get(i).getAsJsonObject().get("tags").getAsString();
            //int length = data.get(i).getAsJsonObject().get("length").getAsInt();
            String time = data.get(i).getAsJsonObject().get("time").getAsString();
            String text = data.get(i).getAsJsonObject().get("text").getAsString();

            // Removes pre-existing Pastebin results cached in search engines
            // and replaces them with more detailed PSBDMP results.
            results.remove("https://pastebin.com/" + id);
            results.add("https://pastebin.com/" + id
                    + (tags.equals("none") ? "" : " (tags=" + tags + ")")
                    + " (" + time + ")"
                    + " (\"" + text + "\")");
        }
    }

    public static String getWebsiteSrc(String website/*, String searchEngine*/) {
        try {
            URL url = new URL(website);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestProperty("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/106.0.0.0 Safari/537.36");
            con.setRequestProperty("content-type", "application/json; utf-8");
            con.setRequestProperty("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
            con.setRequestProperty("accept-language", "en-US,en;q=0.9");
            con.setRequestProperty("upgrade-insecure-requests", "1");
            con.setRequestProperty("dnt", "1");
            con.setRequestProperty("cache-control", "max-age=0");
            con.setRequestProperty("sec-ch-ua", "\"Chromium\";v=\"106\", \"Google Chrome\";v=\"106\", \"Not;A=Brand\";v=\"99\"");
            con.setRequestProperty("sec-ch-ua-mobile", "?0");
            con.setRequestProperty("sec-ch-ua-platform", "\"Windows\"");
            con.setRequestProperty("sec-gpc", "1");
            con.setConnectTimeout(2000);
            con.setReadTimeout(2000);

            // Tests the connection before grabbing the source code.
            // If we're being blocked, return not found and move on.
            try {
                con.getInputStream();
            } catch (IOException ignored) {
//                switch (searchEngine) {
//                    case "google" -> {
//                        googleBlocking = true;
//                        return "Blocked";
//                    }
//                    case "yandex" -> {
//                        yandexBlocking = true;
//                        return "Blocked";
//                    }
//                    case "brave" -> {
//                        braveBlocking = true;
//                        return "Blocked";
//                    }
//                }
            }

            // Grabs the website's source code and returns it in String form.
            InputStream input = con.getInputStream();
            String websiteSrc = getStringFromStream(input);
            con.disconnect();
            return websiteSrc;

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return "Not found";
    }

    public static List<String> parseLinks(String website, String html) {
        List<String> ignoredResults = Arrays.asList("bing.com", "microsoft.com", "googleweblight.com", "yandex.com",
                "yandex.net", "yandex.ru", "brave.com", "hackerone.com", "githubusercontent.com", "docs.github.com",
                "gist.github.com/en/github", "gist.github.com/auth/github", "gist.github.com/fluidicon.png",
                "amazonaws.com", "github.blog", "github.co/", "githubassets.com", "githubstatus.com",
                "services.github.com", "support.github.com", "duckduckgo.com", "directleaks.to/tags/");
        List<String> ignoredResultsExact = Arrays.asList("https://en.wikipedia.org", "https://github.com",
                "https://github.com/about", "https://github.com/pricing", "https://github.com/security");

        List<String> results = new ArrayList<>();
        Pattern pattern = Pattern.compile("href=\"(/url\\?q=)?http.://"
                + (website.equals("") ? "" : website.replace(".", "\\.") + "/") + ".*?\"");
        Matcher matcher = pattern.matcher(html);

        while (matcher.find()) {
            // Clean up the link we found.
            String match = matcher.group();
            match = match.replace("/url?q=", "");
            match = match.replaceAll("&amp;.*", "");
            match = match.replace("href=\"", "");
            match = match.replaceAll("\"", "");
            match = match.replace("//www.", "//");
            match = match.replaceAll("/$", "");

            // Ignores redundant search results.
            if (ignoredResults.stream().anyMatch(match::contains)
                    || ignoredResultsExact.stream().anyMatch(match::equals)) {
                continue;
            }

            results.add(match);
        }

        return results;
    }

    public static String getStringFromStream(InputStream is) {
        StringBuilder sb = new StringBuilder();
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String line;

        try {
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

        } catch (IOException e) {
            e.printStackTrace();

        } finally {
            try {
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return sb.toString();
    }
}
