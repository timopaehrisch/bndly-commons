/*-
 * #%L
 * iCal Impl
 * %%
 * Copyright (C) 2013 - 2020 Cybercon GmbH
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.List;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;


/**
 * Created by alexp on 13.05.15.
 */
public class Main {
    public static void main(String[] args) {
        String sm = "";
        String k = "This value type is used to identify, values that specify a precise calendar date, and time of day. \nThis value type is used to, identify values that specify, a precise calendar date and time of day. This value type is used to identify values that specify a precise calendar date and time of day. This value type is, used to identify values that, specify a precise calendar date and time of day. \nThis value type is used to identify values that specify a precise calendar date and time of day. This value type is used to identify values that specify a precise calendar date and time of day. This value type is used to identify values that specify a precise calendar date and time of day. \nThis value type is used to identify values that specify a precise calendar date and time of day. This value type is used to identify values that specify a precise calendar date and time of day. \nThis value type is used to identify values that specify a precise calendar date and time of day. This value type is used to identify values that specify a precise calendar date and time of day. This value type is used to identify values that specify a precise calendar date and time of day. \nThis value type is used to identify values that specify a precise calendar date and time of day. This value type is used to identify values that specify a precise calendar date and time of day. \nThis value type is used to identify values that specify a precise calendar date and time of day. This value type is used to identify values that specify a precise calendar date and time of day. This value type is used to identify values that specify a precise calendar date and time of day. This value type is used to identify values that specify a precise calendar date and time of day.";

//        k = escapeLineFeeds(k);
//        k = escapeChars(k, ",");
//        k = escapeChars(k, System.lineSeparator());
//        k = escapeChars(k, "\n");
//        k = escapeChars(k, ",");

//        String t = transformToBlockSize(sm, 74);
//        String z = transformToBlockSize("DESCRIPTION:" + k, 74);
//
//        System.out.println("sm > t:\n" + t);
//        System.out.println("k > z:\n" + z);

//        List<String> sts = new ArrayList<String>();
//        sts.add("MO");
//        sts.add("TU");
//        sts.add("FR");
//        sts.add("SU");
//
//
//        try {
//            addIntegerArray(new int[]{8,9,10,11,13,15,46,48,23});
//            System.out.println();
//
//            addDayArray(sts);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        String mailADD = "bndly@cybercon.de";

        try {
            if(isValidEmailAddress(mailADD)){
                URI mail = new URI("mailto:" + mailADD);

                URI mm = new URI("mailto", mailADD, null);

                System.out.println("URI:" + mail.toString());
                System.out.println("Scheme:" + mail.getScheme());
                System.out.println("\n\nURI -mm:" + mm.toString());
//                System.out.println("Authority:" + mail.get^);
            }
            else {
                throw new AddressException("No valid e-mail address!");
            }
        }
        catch (AddressException e) {
            e.printStackTrace();
        }
        catch (URISyntaxException use){
            use.printStackTrace();
        }
//        catch (MalformedURLException mux){
//            mux.printStackTrace();
//        }

    }

    private static String transformToBlockSize(String s, int blockSize) {
        s = s.trim();
        StringBuilder sb = new StringBuilder();

        for(int i = 0; i < s.length(); i += blockSize){

            if(i+blockSize < s.length()) {
                sb.append(s.substring(i, i+blockSize));
                sb.append(System.lineSeparator()).append(" ");
            }
            else {
                sb.append(s.substring(i, s.length()-1));
            }
        }

        return sb.toString();
    }

    private static String escapeLineFeeds(String a) {
        return a.replace(System.lineSeparator(), "\\n");
    }

    private static String escapeChars(String a, String escapingChars) {
        if(System.lineSeparator().equalsIgnoreCase(escapingChars)
                || "\n".equalsIgnoreCase(escapingChars)) {
            return escapeLineFeeds(a);
        }

        return a.replace(escapingChars, "\\".concat(escapingChars));
    }

    private static void addIntegerArray(int[] iA) throws IOException {
        for(int i = 0; i < iA.length; i++){
            System.out.print(iA[i]);

            if(i+1 != iA.length){
                System.out.print(",");
            }
        }
    }

    private static void addDayArray(List<String> days) throws IOException{
        Iterator it = days.iterator();
        while(it.hasNext()){
            System.out.print(it.next().toString());

            if( it.hasNext() ){
                System.out.print(",");
            }
        }
    }

    private static boolean isValidEmailAddress(String email) throws AddressException {
        boolean result = true;
        try {
            InternetAddress test;

            InternetAddress emailAddr = new InternetAddress(email);
            emailAddr.validate();
        } catch (AddressException ex) {
            result = false;
        }
        return result;
    }
}
