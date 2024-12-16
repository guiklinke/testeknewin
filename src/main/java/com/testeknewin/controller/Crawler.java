package main.java.com.testeknewin.controller;

import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;

import main.java.com.testeknewin.model.Noticia;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;


public class Crawler {

    private final String URL_SITE = "https://www.infomoney.com.br/mercados/";

    private WebDriver driver;

    public ArrayList<Noticia> noticias;

    public ArrayList<WebElement> lidas;

    private int TOTAL;

    private int CONTADOR = 0;

    public Crawler(int TOTAL) {
        this.TOTAL = TOTAL;
        this.noticias = new ArrayList<Noticia>();
        this.lidas = new ArrayList<WebElement>();
    }


    public void start() {
        config();
        getLinks();
        printNews();
        destroy();
    }


    private void destroy() {
        this.driver.quit();
    }


    public void getLinks() {

        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("window.scrollBy(0, 1000)");

        try {
            Thread.sleep(5000);

            ArrayList<WebElement> links = new ArrayList<WebElement>(driver.findElements(By.xpath("//span[contains(@class, 'hl-title') and contains(@class, 'hl-title-2')]")));

            while (CONTADOR < TOTAL && !links.isEmpty()) {

                getData(links);

                if(!driver.toString().contains("(null)")) {
                    WebElement button = this.driver.findElement(By.xpath("/html/body/div[4]/div[4]/div/div/div[11]/span/button"));
                    goToElement(button);
                    Thread.sleep(2000);

                    js.executeScript("arguments[0].click();", button);

                    Thread.sleep(6000);
                    links = new ArrayList<WebElement>(this.driver.findElements(By.xpath("//span[contains(@class, 'hl-title') and contains(@class, 'hl-title-2')]")));
                    links = new ArrayList<WebElement>(links.stream().filter(n -> !lidas.contains(n)).toList());
                    goToElement(links.get(0));
                }else {
                    break;
                }

            }

        } catch (Exception exception) {
            exception.printStackTrace();
            destroy();
        }

    }


    private void getData(ArrayList<WebElement> links) {
        try {

            for (WebElement link: links) {

                if (CONTADOR >= TOTAL) {
                    break;
                }

                goToElement(link);
                String url = link.findElement(By.xpath(".//a[not(@id)]")).getAttribute("href");
                ((JavascriptExecutor) this.driver).executeScript("window.open()");
                ArrayList<String> abas = new ArrayList<String>(driver.getWindowHandles());
                this.driver.switchTo().window(abas.get(1));
                this.driver.get(url);

                Noticia noticia = new Noticia();
                noticia.setUrl(url);

                WebDriverWait wait = new WebDriverWait(this.driver, 10);
                WebElement tituloElement = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//h1[contains(@class, 'page-title-1')]")));
                if(tituloElement != null) {
                    goToElement(tituloElement);
                    noticia.setTitulo(tituloElement.getText());
                }

                WebElement subtituloElement = this.driver.findElement(By.xpath("//p[contains(@class, 'article-lead')]"));
                if(subtituloElement != null) {
                    goToElement(subtituloElement);
                    noticia.setSubtitulo(subtituloElement.getText());
                }

                WebElement autorElement = this.driver.findElement(By.xpath("//span[contains(@class, 'author-name')]"));
                if(autorElement != null) {
                    goToElement(autorElement);
                    String autor = autorElement.getText();
                    autor = autor.substring(0,4).equals("Por ") ? autor.substring(4, autor.length()) : autor;
                    noticia.setAutor(autor);
                }

                WebElement dataElement = this.driver.findElement(By.xpath("//time[contains(@class, 'entry-date') and contains(@class, 'published')]"));
                if(tituloElement != null) {
                    goToElement(dataElement);
                    OffsetDateTime dt = OffsetDateTime.parse(dataElement.getAttribute("datetime"));
                    Date date = new Date(dt.toInstant().toEpochMilli());
                    String dataFormatada = new SimpleDateFormat("dd/MM/yyyy HH:mm").format(date);
                    noticia.setDataPublicacao(dataFormatada);
                }

                WebElement contentElement = this.driver.findElement(By.xpath("//div[contains(@class, 'article-content')]"));
                ArrayList<WebElement> trechos = new ArrayList<WebElement>(contentElement.findElements(By.xpath(".//p[not(@id)] | .//h2[not(@id)]")));
                goToElement(trechos.get(0));
                String conteudo = "";
                for(int i = 0; i < trechos.size(); i++) {
                    conteudo += " "+trechos.get(i).getText();
                }
                conteudo = conteudo.replace("\n", " ").trim();
                noticia.setConteudo(conteudo);


                this.lidas.add(link);
                this.noticias.add(noticia);
                this.driver.close();
                this.driver.switchTo().window(abas.get(0));
                CONTADOR = CONTADOR + 1;
            }

        }catch (Exception exception) {
            exception.printStackTrace();
            destroy();
        }

    }


    public void printNews() {

        this.noticias.forEach(n -> System.out.println(String.format(
                                "Url: %s\nData da publicação: %s\nTítulo: %s\nSubtítulo: %s\nAutor: %s\nConteúdo: %s\n",
                                n.getUrl(), n.getDataPublicacao().toString(), n.getTitulo(), n.getSubtitulo(), n.getAutor(), n.getConteudo()
                        )
                )
        );
    }


    private void goToElement(WebElement element) {
        ((JavascriptExecutor) this.driver).executeScript("arguments[0].scrollIntoView(true);", element);
        if (!element.isDisplayed()) {
            Actions actions = new Actions(driver);
            actions.moveToElement(element);
        }
    }


    private void config() {
        Properties prop=new Properties();
        try {
            FileInputStream ip = new FileInputStream("src/config.properties");
            prop.load(ip);
        } catch (IOException exception) {
            exception.printStackTrace();
        }

        String cd = prop.getProperty("chromedriverpath");
        System.setProperty("webdriver.chrome.driver", cd);

        ChromeOptions options = new ChromeOptions();

        DesiredCapabilities capabilities = new DesiredCapabilities();
        capabilities.setAcceptInsecureCerts(true);
        capabilities.setJavascriptEnabled(true);
        options.setCapability(ChromeOptions.CAPABILITY, capabilities);
        options.setCapability(CapabilityType.ACCEPT_SSL_CERTS, true);
        options.setCapability(CapabilityType.SUPPORTS_FINDING_BY_CSS, true);

        this.driver = new ChromeDriver(options);
        this.driver.manage().window().maximize();
        this.driver.get(URL_SITE);
    }

}
