package gerador.obtemoportunidadelinkedin.passo.impl;


import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
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
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

        try {
            // Acessar a página de login do LinkedIn
            driver.get("https://www.linkedin.com/login");

            // Fazer login
            WebElement emailField = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("username")));
            emailField.sendKeys("paulofore@gmail.com");

            WebElement passwordField = driver.findElement(By.id("password"));
            passwordField.sendKeys("xi5*4NDGrb^+Z6T");
            passwordField.sendKeys(Keys.RETURN);

            // Navegar para a página de busca de vagas
            driver.get("https://www.linkedin.com/jobs");
            wait.until(ExpectedConditions.or(
            	ExpectedConditions.urlContains("linkedin.com/jobs"),
            	ExpectedConditions.presenceOfElementLocated(By.tagName("body"))
            ));
            salvarCopiaPagina("jobs-inicial");

            // Inserir termo de pesquisa e buscar (com aprendizado/resiliência)
            boolean pesquisouNoCampo = false;
            try {
            	WebElement searchBox = encontraCampoBusca(wait);
            	searchBox.click();
            	searchBox.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.DELETE);
            	searchBox.sendKeys(palavraPesquisaCorrente.getPalavra());
            	searchBox.sendKeys(Keys.RETURN);
            	pesquisouNoCampo = true;
            } catch (Exception ex) {
            	System.out.println("Campo de busca não encontrado. Tentando navegação direta por URL de pesquisa...");
            	salvarCopiaPagina("erro-sem-campo-busca");
            }

            if (!pesquisouNoCampo) {
            	String termo = URLEncoder.encode(palavraPesquisaCorrente.getPalavra(), StandardCharsets.UTF_8);
            	driver.get("https://www.linkedin.com/jobs/search/?keywords=" + termo + "&sortBy=DD");
            }

            // Esperar resultados de pesquisa
            wait.until(ExpectedConditions.or(
            	ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector("a.job-card-container__link")),
            	ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector("li.scaffold-layout__list-item")),
            	ExpectedConditions.urlContains("/jobs/search")
            ));
            TimeUnit.SECONDS.sleep(4);
            salvarCopiaPagina("jobs-resultado");

            this.saidaListaOportunidade = new ArrayList<OportunidadeLinkedin>();
            adicionaItens(palavraPesquisaCorrente);

			for (int pagina = 2; pagina <= 15; pagina++) {
				try {
					WebElement button = driver.findElement(By.xpath("//button[@aria-label='Página " + pagina + "']"));
					if (button != null) {
						button.click();
						TimeUnit.SECONDS.sleep(4);
						adicionaItens(palavraPesquisaCorrente);
					}
				} catch (NoSuchElementException e) {
					// fim da paginação
				}

			}
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            salvarCopiaPagina("erro-geral");
            return false;
        } finally {
            // Fechar o navegador
            driver.quit();
        }
        
	}

	private WebElement encontraCampoBusca(WebDriverWait wait) {
		By[] tentativas = {
			By.cssSelector("input.jobs-search-box__text-input"),
			By.cssSelector("input.jobs-search-box__keyboard-text-input"),
			By.cssSelector("input[aria-label*='cargo'], input[aria-label*='título'], input[aria-label*='Pesquisar'], input[aria-label*='title'], input[aria-label*='Search']"),
			By.cssSelector("input[id*='jobs-search-box-keyword-id']"),
			By.xpath("//input[contains(@placeholder,'Pesquisar') or contains(@placeholder,'cargo') or contains(@placeholder,'title') or contains(@placeholder,'Search')]")
		};

		for (By localizador : tentativas) {
			try {
				return wait.until(ExpectedConditions.elementToBeClickable(localizador));
			} catch (Exception ignored) {
			}
		}

		List<WebElement> inputs = driver.findElements(By.cssSelector("input[type='text'], input[type='search']"));
		return inputs.stream()
				.filter(WebElement::isDisplayed)
				.max(Comparator.comparingInt(this::pontuacaoCampoBusca))
				.filter(el -> pontuacaoCampoBusca(el) > 0)
				.orElseThrow(() -> new NoSuchElementException("Campo de busca de vagas do LinkedIn não encontrado"));
	}

	private int pontuacaoCampoBusca(WebElement el) {
		String texto = (el.getAttribute("aria-label") + " " + el.getAttribute("placeholder") + " " + el.getAttribute("id") + " " + el.getAttribute("class")).toLowerCase();
		int pontos = 0;
		if (texto.contains("cargo") || texto.contains("vaga") || texto.contains("palavra")) pontos += 4;
		if (texto.contains("title") || texto.contains("keyword") || texto.contains("search")) pontos += 4;
		if (texto.contains("jobs")) pontos += 2;
		if (Boolean.TRUE.equals(((JavascriptExecutor) driver).executeScript("return arguments[0]===document.activeElement", el))) pontos += 1;
		return pontos;
	}

	private void salvarCopiaPagina(String marcador) {
		if (driver == null) return;
		try {
			Path pasta = Paths.get(System.getProperty("java.io.tmpdir"), "linkedin-debug");
			Files.createDirectories(pasta);
			String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
			Path arquivo = pasta.resolve(ts + "-" + marcador + ".html");
			Files.writeString(arquivo, driver.getPageSource(), StandardCharsets.UTF_8);
			System.out.println("Cópia da página salva em: " + arquivo);
		} catch (Exception e) {
			System.out.println("Falha ao salvar cópia da página: " + e.getMessage());
		}
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
    	List<WebElement> jobs = driver.findElements(By.className("job-card-container__link"));
    	 for (WebElement job : jobs) {
         	OportunidadeLinkedin novo = new OportunidadeLinkedin();
             job.click();
             TimeUnit.SECONDS.sleep(2);

             try {
                 WebElement description = driver.findElement(By.id("job-details"));
                 System.out.println(description.getText());
                 novo.setDescricao(description.getText());
                 System.out.println();
                 
                 WebElement jobLinkElement = driver.findElement(By.xpath("//div[contains(@class, 'job-details-jobs-unified-top-card__job-title')]//h1[@class='t-24 t-bold inline']/a"));
                 String jobLinkUrl = jobLinkElement.getAttribute("href");
                 String baseUrl = jobLinkUrl.split("\\?")[0];
                 System.out.println("Job Link URL: " + baseUrl);
                 
                 WebElement jobTitleElement = driver.findElement(By.xpath("//div[contains(@class, 'job-details-jobs-unified-top-card__job-title')]//h1[@class='t-24 t-bold inline']/a"));
                 String jobTitleText = jobTitleElement.getText();
                 System.out.println("Job Title: " + jobTitleText);
                 
                 WebElement companyNameElement = driver.findElement(By.xpath("//div[contains(@class, 'job-details-jobs-unified-top-card__company-name')]//a"));
                 String companyNameText = companyNameElement.getText();
                 System.out.println("Company Name: " + companyNameText);
                 
                 WebElement diasElement = driver.findElement(By.xpath("(//div[contains(@class, 'job-details-jobs-unified-top-card__primary-description-container')]//span[@class='tvm__text tvm__text--low-emphasis'])[3]"));
                 String diasText = diasElement.getText();
                 System.out.println("Tempo: " + diasText);

                 String candidaturasText = "0";
                 try {
                	 WebElement candidaturasElement = driver.findElement(By.xpath("(//div[contains(@class, 'job-details-jobs-unified-top-card__primary-description-container')]//span[@class='tvm__text tvm__text--low-emphasis'])[5]"));
                	 candidaturasText = candidaturasElement.getText();
                 } catch (NoSuchElementException err) {
                	 System.out.println("Sem candidatoras");
                 }
                 
                 String modelo = "";
                 try {
                     WebElement remotoElement = driver.findElement(By.xpath("//li[contains(@class, 'job-details-jobs-unified-top-card__job-insight--highlight')]//span[@aria-hidden='true']"));
                     modelo = remotoElement.getText();
                 } catch (NoSuchElementException err) {
                	 System.out.println("Sem remoto");
                 }
                 
                 System.out.println("Candidaturas: " + candidaturasText);
                 System.out.println("Modelo: " + modelo);
                 
                 novo.setDescricao(description.getText());
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


}
