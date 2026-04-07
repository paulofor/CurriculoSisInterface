package gerador.obtemoportunidadelinkedin.passo.impl;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import br.com.gersis.loopback.modelo.OportunidadeLinkedin;
import br.com.gersis.loopback.modelo.PalavraRaiz;
import gerador.obtemoportunidadelinkedin.passo.AcessaLinkedIn;



public class AcessaLinkedInImpl extends AcessaLinkedIn {

	WebDriver driver = null;
	
	/*
	 * Trocar o Driver do Chrome:
	 * 
	 * /usr/local/bin/
	 * https://googlechromelabs.github.io/chrome-for-testing/
	 * 	https://storage.googleapis.com/chrome-for-testing-public/137.0.7151.119/win64/chromedriver-win64.zip
	 */
	
	
	@Override
	protected boolean executaCustom(PalavraRaiz palavraPesquisaCorrente) {
		configuraChromeDriver();

        // Inicializar o navegador
        driver = new ChromeDriver();
        WebDriverWait wait = new WebDriverWait(driver, 20);

        try {
            // Acessar a página de login do LinkedIn
            driver.get("https://www.linkedin.com/login");

            // Fazer login
            WebElement emailField = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("username")));
            emailField.sendKeys("paulofore@gmail.com");

            WebElement passwordField = driver.findElement(By.id("password"));
            passwordField.sendKeys("xi5*4NDGrb^+Z6T");
            passwordField.sendKeys(Keys.RETURN);

            // Navegar para a página de busca de vagas de forma resiliente
            pesquisarVagasInteligente(wait, palavraPesquisaCorrente.getPalavra());

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

	private void pesquisarVagasInteligente(WebDriverWait wait, String palavra) throws InterruptedException {
		String termo = palavra == null ? "" : palavra.trim();
		if (termo.isEmpty()) {
			throw new IllegalArgumentException("Palavra de busca vazia");
		}

		// Estratégia 1: URL direta de busca (menos sensível a mudanças de HTML).
		String termoUrl = termo.replace(" ", "%20");
		driver.get("https://www.linkedin.com/jobs/search/?keywords=" + termoUrl);
		TimeUnit.SECONDS.sleep(3);

		// Se a página carregou resultados/lista, não depende de campo de busca.
		if (temResultadosOuLista()) {
			return;
		}

		// Estratégia 2: abrir jobs e tentar campo de busca com múltiplos seletores.
		driver.get("https://www.linkedin.com/jobs");
		WebElement searchBox = encontraCampoBusca(wait);
		searchBox.clear();
		searchBox.sendKeys(termo);
		searchBox.sendKeys(Keys.RETURN);
		TimeUnit.SECONDS.sleep(3);

		if (!temResultadosOuLista()) {
			throw new NoSuchElementException("Não foi possível navegar para resultados de vagas no LinkedIn");
		}
	}

	private boolean temResultadosOuLista() {
		By[] indicadores = {
			By.cssSelector("ul.jobs-search__results-list"),
			By.cssSelector("div.scaffold-layout__list"),
			By.cssSelector("li.jobs-search-results__list-item"),
			By.cssSelector("a.job-card-container__link"),
			By.cssSelector("div.job-card-list")
		};
		for (By indicador : indicadores) {
			if (!driver.findElements(indicador).isEmpty()) {
				return true;
			}
		}
		return false;
	}

	private WebElement encontraCampoBusca(WebDriverWait wait) {
		By[] tentativas = {
			By.cssSelector("input.jobs-search-box__text-input"),
			By.cssSelector("input.jobs-search-box__keyboard-text-input"),
			By.cssSelector("input[aria-label*='Cargo']"),
			By.cssSelector("input[aria-label*='título']"),
			By.cssSelector("input[aria-label*='Title']"),
			By.cssSelector("input[placeholder*='Pesquisar']"),
			By.cssSelector("input[placeholder*='Search']"),
			By.xpath("//input[contains(@aria-label,'Pesquisar') or contains(@aria-label,'Search by title')]"),
			By.xpath("//input[contains(@id,'jobs-search-box-keyword-id')]")
		};

		for (By localizador : tentativas) {
			try {
				return wait.until(ExpectedConditions.elementToBeClickable(localizador));
			} catch (Exception ignored) {
			}
		}

		throw new NoSuchElementException("Campo de busca de vagas do LinkedIn não encontrado em nenhuma estratégia");
	}

	private void configuraChromeDriver() {
		String driverConfigurado = System.getenv("WEBDRIVER_CHROME_DRIVER");
		if (driverConfigurado != null && !driverConfigurado.isBlank()) {
			System.setProperty("webdriver.chrome.driver", driverConfigurado);
			return;
		}

		String[] candidatos = {
			"/usr/local/bin/chromedriver",
			"/usr/bin/chromedriver",
			"/usr/local/bin/chromedriver.exe",
			"C:/chromedriver/chromedriver.exe"
		};

		for (String candidato : candidatos) {
			Path caminho = Paths.get(candidato);
			if (Files.exists(caminho) && Files.isRegularFile(caminho)) {
				System.setProperty("webdriver.chrome.driver", caminho.toAbsolutePath().toString());
				return;
			}
		}

		Path baixado = baixaChromeDriverSeNecessario();
		if (baixado != null) {
			System.setProperty("webdriver.chrome.driver", baixado.toAbsolutePath().toString());
		}
	}

	private Path baixaChromeDriverSeNecessario() {
		String plataforma = getPlataformaChromeDriver();
		if (plataforma == null) {
			return null;
		}

		String majorChrome = getMajorChromeInstalado();
		String versaoDriver = obtemVersaoDriver(majorChrome);
		if (versaoDriver == null || versaoDriver.isBlank()) {
			return null;
		}

		String nomeExecutavel = plataforma.startsWith("win") ? "chromedriver.exe" : "chromedriver";
		Path destinoBase = Paths.get(System.getProperty("java.io.tmpdir"), "webdriver", "chrome", versaoDriver, plataforma);
		Path destinoExecutavel = destinoBase.resolve(nomeExecutavel);
		if (Files.exists(destinoExecutavel) && Files.isRegularFile(destinoExecutavel)) {
			return destinoExecutavel;
		}

		String urlZip = "https://storage.googleapis.com/chrome-for-testing-public/" + versaoDriver + "/" + plataforma + "/chromedriver-" + plataforma + ".zip";
		try {
			Files.createDirectories(destinoBase);
			downloadEExtraiZip(urlZip, destinoBase, nomeExecutavel);
			if (Files.exists(destinoExecutavel) && Files.isRegularFile(destinoExecutavel)) {
				destinoExecutavel.toFile().setExecutable(true);
				return destinoExecutavel;
			}
		} catch (Exception e) {
			System.out.println("Não foi possível baixar automaticamente o chromedriver: " + e.getMessage());
		}
		return null;
	}

	private String obtemVersaoDriver(String majorChrome) {
		String endpointVersao = "https://storage.googleapis.com/chrome-for-testing-public/LATEST_RELEASE_STABLE";
		if (majorChrome != null) {
			endpointVersao = "https://storage.googleapis.com/chrome-for-testing-public/LATEST_RELEASE_" + majorChrome;
		}

		String versao = getTexto(endpointVersao);
		if ((versao == null || versao.isBlank()) && majorChrome != null) {
			versao = getTexto("https://storage.googleapis.com/chrome-for-testing-public/LATEST_RELEASE_STABLE");
		}
		return versao == null ? null : versao.trim();
	}

	private String getTexto(String url) {
		try {
			HttpClient client = HttpClient.newHttpClient();
			HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() >= 200 && response.statusCode() < 300) {
				return response.body();
			}
		} catch (Exception e) {
			System.out.println("Falha ao consultar URL de versão do chromedriver: " + url);
		}
		return null;
	}

	private void downloadEExtraiZip(String urlZip, Path destinoBase, String nomeExecutavel) throws IOException, InterruptedException {
		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest.newBuilder().uri(URI.create(urlZip)).GET().build();
		HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
		if (response.statusCode() < 200 || response.statusCode() >= 300) {
			throw new IOException("Download retornou status " + response.statusCode());
		}

		try (InputStream input = response.body(); ZipInputStream zip = new ZipInputStream(input)) {
			ZipEntry entry;
			while ((entry = zip.getNextEntry()) != null) {
				if (entry.isDirectory()) {
					continue;
				}
				if (!entry.getName().endsWith(nomeExecutavel)) {
					continue;
				}
				Path arquivoDestino = destinoBase.resolve(nomeExecutavel);
				try (OutputStream output = Files.newOutputStream(arquivoDestino)) {
					zip.transferTo(output);
				}
				break;
			}
		}
	}

	private String getMajorChromeInstalado() {
		String[] comandos = {
			"google-chrome --version",
			"google-chrome-stable --version",
			"chromium-browser --version",
			"chromium --version"
		};
		for (String cmd : comandos) {
			String saida = executaComandoVersao(cmd);
			String major = extraiMajor(saida);
			if (major != null) {
				return major;
			}
		}
		return null;
	}

	private String executaComandoVersao(String comando) {
		try {
			Process processo = new ProcessBuilder("bash", "-lc", comando).redirectErrorStream(true).start();
			String saida;
			try (InputStream input = processo.getInputStream()) {
				saida = new String(input.readAllBytes());
			}
			processo.waitFor();
			return saida;
		} catch (Exception e) {
			return null;
		}
	}

	private String extraiMajor(String texto) {
		if (texto == null) {
			return null;
		}
		Matcher matcher = Pattern.compile("(\\d+)\\.").matcher(texto);
		if (matcher.find()) {
			return matcher.group(1);
		}
		return null;
	}

	private String getPlataformaChromeDriver() {
		String os = System.getProperty("os.name").toLowerCase();
		String arch = System.getProperty("os.arch").toLowerCase();

		if (os.contains("win")) {
			return "win64";
		}
		if (os.contains("linux")) {
			return "linux64";
		}
		if (os.contains("mac")) {
			return arch.contains("aarch64") || arch.contains("arm64") ? "mac-arm64" : "mac-x64";
		}
		return null;
	}

	
	
	private void adicionaItens(PalavraRaiz palavraRaiz) throws InterruptedException {
		By[] seletoresCards = {
			By.className("job-card-container__link"),
			By.cssSelector("li.jobs-search-results__list-item a"),
			By.cssSelector("div.job-card-container a"),
			By.cssSelector("a.job-card-list__title")
		};
		List<WebElement> jobs = localizarElementosPrimeiroDisponivel(seletoresCards);
    	 for (WebElement job : jobs) {
         	OportunidadeLinkedin novo = new OportunidadeLinkedin();
             job.click();
             TimeUnit.SECONDS.sleep(2);

             try {
                 String descricaoTexto = obterTextoPrimeiroDisponivel(
					By.id("job-details"),
					By.cssSelector("div.jobs-description-content__text"),
					By.cssSelector("article.jobs-description"),
					By.cssSelector("div.jobs-box__html-content")
				);
                 System.out.println(descricaoTexto);
                 System.out.println();
                 
                 WebElement jobLinkElement = localizarElementoPrimeiroDisponivel(
					By.xpath("//div[contains(@class, 'job-details-jobs-unified-top-card__job-title')]//h1//a"),
					By.cssSelector("h1 a"),
					By.cssSelector("a.job-details-jobs-unified-top-card__job-title-link")
				);
                 String jobLinkUrl = jobLinkElement.getAttribute("href");
                 String baseUrl = jobLinkUrl == null ? "" : jobLinkUrl.split("\\?")[0];
                 System.out.println("Job Link URL: " + baseUrl);
                 
                 WebElement jobTitleElement = localizarElementoPrimeiroDisponivel(
					By.xpath("//div[contains(@class, 'job-details-jobs-unified-top-card__job-title')]//h1"),
					By.cssSelector("h1.t-24"),
					By.cssSelector("h1")
				);
                 String jobTitleText = jobTitleElement.getText();
                 System.out.println("Job Title: " + jobTitleText);
                 
                 WebElement companyNameElement = localizarElementoPrimeiroDisponivel(
					By.xpath("//div[contains(@class, 'job-details-jobs-unified-top-card__company-name')]//a"),
					By.cssSelector("div.job-details-jobs-unified-top-card__company-name a"),
					By.cssSelector("a.topcard__org-name-link"),
					By.cssSelector("span.jobs-unified-top-card__company-name")
				);
                 String companyNameText = companyNameElement.getText();
                 System.out.println("Company Name: " + companyNameText);
                 
                 String diasText = obterTextoPrimeiroDisponivel(
					By.xpath("(//div[contains(@class, 'job-details-jobs-unified-top-card__primary-description-container')]//span[contains(@class,'low-emphasis')])[3]"),
					By.cssSelector("span.jobs-unified-top-card__posted-date"),
					By.xpath("//span[contains(.,'há ') or contains(.,'ago') or contains(.,'dia')]")
				);
                 System.out.println("Tempo: " + diasText);

                 String candidaturasText = "0";
                 try {
                	 WebElement candidaturasElement = localizarElementoPrimeiroDisponivel(
						By.xpath("(//div[contains(@class, 'job-details-jobs-unified-top-card__primary-description-container')]//span[contains(@class,'low-emphasis')])[5]"),
						By.xpath("//span[contains(translate(.,'CANDIDATURAS','candidaturas'),'candidatura') or contains(.,'applicant')]")
					);
                	 candidaturasText = candidaturasElement.getText();
                 } catch (NoSuchElementException err) {
                	 System.out.println("Sem candidatoras");
                 }
                 
                 String modelo = "";
                 try {
                     WebElement remotoElement = localizarElementoPrimeiroDisponivel(
						By.xpath("//li[contains(@class, 'job-details-jobs-unified-top-card__job-insight--highlight')]//span[@aria-hidden='true']"),
						By.xpath("//li[contains(.,'Remoto') or contains(.,'Híbrido') or contains(.,'Presencial') or contains(.,'Remote') or contains(.,'Hybrid') or contains(.,'On-site')]")
					);
                     modelo = remotoElement.getText();
                 } catch (NoSuchElementException err) {
                	 System.out.println("Sem remoto");
                 }
                 
                 System.out.println("Candidaturas: " + candidaturasText);
                 System.out.println("Modelo: " + modelo);
                 
	                 novo.setDescricao(descricaoTexto);
                 novo.setVolume(candidaturasText);
                 novo.setTempo(diasText);
                 novo.setTitulo(jobTitleText);
                 novo.setUrl(baseUrl);
                 novo.setEmpresa(companyNameText);
                 novo.setPalavraRaizId("" + palavraRaiz.getIdInteger());
                 novo.setModelo(modelo);
                 
                 
                 saidaListaOportunidade.add(novo);
             } catch (Exception e) {
            	 e.printStackTrace();
                 System.out.println("Não foi possível extrair a descrição da vaga.");
             }
         }
    }

	private List<WebElement> localizarElementosPrimeiroDisponivel(By... seletores) {
		for (By seletor : seletores) {
			List<WebElement> elementos = driver.findElements(seletor);
			if (!elementos.isEmpty()) {
				return elementos;
			}
		}
		return new ArrayList<>();
	}

	private WebElement localizarElementoPrimeiroDisponivel(By... seletores) {
		for (By seletor : seletores) {
			List<WebElement> elementos = driver.findElements(seletor);
			if (!elementos.isEmpty()) {
				return elementos.get(0);
			}
		}
		throw new NoSuchElementException("Elemento não encontrado com seletores dinâmicos");
	}

	private String obterTextoPrimeiroDisponivel(By... seletores) {
		try {
			return localizarElementoPrimeiroDisponivel(seletores).getText();
		} catch (NoSuchElementException e) {
			return "";
		}
	}


}
