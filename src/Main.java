import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        
        System.out.println("Please Enter the Number of Hosts.");
        int numberOfHosts = sc.nextInt(); 
        for(int i = 0; i < numberOfHosts; i++){
            SimpleWebServerPJE webServer = new SimpleWebServerPJE(i);
            webServer.start();
        }
        sc.close();
    }
}
