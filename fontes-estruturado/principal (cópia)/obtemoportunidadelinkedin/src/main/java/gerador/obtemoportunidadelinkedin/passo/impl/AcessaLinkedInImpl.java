package gerador.obtemoportunidadelinkedin.passo.impl;


import java.util.ArrayList;
import java.util.List;
import java.time.Duration;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

import br.com.gersis.loopback.modelo.OportunidadeLinkedin;
import br.com.gersis.loopback.modelo.PalavraRaiz;
import gerador.obtemoportunidadelinkedin.passo.AcessaLinkedIn;



public class AcessaLinkedInImpl extends AcessaLinkedIn {

	WebDriver driver = null;
	
	
	
	@Override
	protected boolean executaCustom(PalavraRaiz palavraPesquisaCorrente) {
		System.setProperty("webdriver.chrome.driver", "/usr/local/bin/chromedriver");

        // Inicializar o navegador
        driver = new ChromeDriver();

        try {
            // Acessar a página de login do LinkedIn
            driver.get("https://www.linkedin.com/login");

            System.out.println("Faça login manualmente no LinkedIn no navegador aberto.");
            System.out.println("Depois de concluir o login, pressione ENTER aqui para continuar...");
            try (Scanner scanner = new Scanner(System.in)) {
                scanner.nextLine();
            }

            // Espera de segurança para confirmar que saiu da tela de login.
            aguardaLoginConcluido();
            driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);

            // Navegar para a página de busca de vagas
            driver.get("https://www.linkedin.com/jobs");

            // Inserir termo de pesquisa e buscar
            WebElement searchBox = driver.findElement(By.className("jobs-search-box__text-input"));
            searchBox.sendKeys(palavraPesquisaCorrente.getPalavra());
            searchBox.sendKeys(Keys.RETURN);

            // Esperar resultados de pesquisa
            TimeUnit.SECONDS.sleep(5);

            // Coletar descrições de vagas
            

            this.saidaListaOportunidade = new ArrayList<OportunidadeLinkedin>();
            adicionaItens(palavraPesquisaCorrente);

			for (int pagina = 2; pagina <= 15; pagina++) {
				// Localiza o botão pelo atributo aria-label usando XPath
				try {
					WebElement button = driver.findElement(By.xpath("//button[@aria-label='Página " + pagina + "']"));
					if (button != null) {
						button.click();
						TimeUnit.SECONDS.sleep(5);
						adicionaItens(palavraPesquisaCorrente);
					}
				} catch (NoSuchElementException e) {

				}

			}
            
           
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            // Fechar o navegador
            driver.quit();
        }
        
	}


    private void aguardaLoginConcluido() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
        try {
            wait.until((ExpectedCondition<Boolean>) webDriver -> {
                if (webDriver == null) return false;
                String url = webDriver.getCurrentUrl();
                return url != null && !url.contains("/login");
            });
        } catch (TimeoutException e) {
            throw new RuntimeException("Login não foi concluído em 30 segundos após ENTER.", e);
        }
    }
	
	
	private void adicionaItens(PalavraRaiz palavraRaiz) throws InterruptedException {
    	List<WebElement> jobs = driver.findElements(By.className("job-card-container__link"));
    	 for (WebElement job : jobs) {
         	OportunidadeLinkedin novo = new OportunidadeLinkedin();
             job.click();
             TimeUnit.SECONDS.sleep(2);

             try {
                 WebElement description = driver.findElement(By.id("job-details"));
                 //System.out.println("Descrição da Vaga:");
                 //System.out.println(description.getText());
                 novo.setDescricao(description.getText());
                 //System.out.println("----");
                 System.out.println();
                 
                 // Localiza o elemento <a> pela posição no DOM
                 WebElement jobLinkElement = driver.findElement(By.xpath("//div[contains(@class, 'job-details-jobs-unified-top-card__job-title')]//h1[@class='t-24 t-bold inline']/a"));
                 // Extrai o valor do atributo href
                 String jobLinkUrl = jobLinkElement.getAttribute("href");
                 String baseUrl = jobLinkUrl.split("\\?")[0];
                 System.out.println("Job Link URL: " + baseUrl);
                 
                 WebElement jobTitleElement = driver.findElement(By.xpath("//div[contains(@class, 'job-details-jobs-unified-top-card__job-title')]//h1[@class='t-24 t-bold inline']/a"));
                 String jobTitleText = jobTitleElement.getText();
                 System.out.println("Job Title: " + jobTitleText);
                 
                 // Localiza o elemento que contém "Capco" pela posição no DOM
                 WebElement companyNameElement = driver.findElement(By.xpath("//div[contains(@class, 'job-details-jobs-unified-top-card__company-name')]//a"));
                 String companyNameText = companyNameElement.getText();
                 System.out.println("Company Name: " + companyNameText);
                 
                 // Localiza o elemento que contém "há 3 dias" pela posição no DOM
                 WebElement diasElement = driver.findElement(By.xpath("(//div[contains(@class, 'job-details-jobs-unified-top-card__primary-description-container')]//span[@class='tvm__text tvm__text--low-emphasis'])[3]"));
                 String diasText = diasElement.getText();
                 System.out.println("Tempo: " + diasText);

                 // Localiza o elemento que contém "68 candidaturas" pela posição no DOM
                 WebElement candidaturasElement = driver.findElement(By.xpath("(//div[contains(@class, 'job-details-jobs-unified-top-card__primary-description-container')]//span[@class='tvm__text tvm__text--low-emphasis'])[5]"));
                 String candidaturasText = candidaturasElement.getText();
                 System.out.println("Candidaturas: " + candidaturasText);
                 
                 novo.setDescricao(description.getText());
                 novo.setVolume(candidaturasText);
                 novo.setTempo(diasText);
                 novo.setTitulo(jobTitleText);
                 novo.setUrl(baseUrl);
                 novo.setEmpresa(companyNameText);
                 novo.setPalavraRaizId("" + palavraRaiz.getIdInteger());
                 
                 
                 saidaListaOportunidade.add(novo);
             } catch (Exception e) {
                 System.out.println("Não foi possível extrair a descrição da vaga.");
             }
         }
    }


}

