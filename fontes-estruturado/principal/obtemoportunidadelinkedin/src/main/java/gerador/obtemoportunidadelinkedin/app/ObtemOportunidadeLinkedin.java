package gerador.obtemoportunidadelinkedin.app;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import gerador.obtemoportunidadelinkedin.passo.*;
import gerador.obtemoportunidadelinkedin.passo.impl.*;
import br.com.gersis.daobase.comum.DaoBaseComum;

public class ObtemOportunidadeLinkedin {

	private static String UrlLoopback = "";

	public static void main(String[] args) {
		System.out.print("ObtemOportunidadeLinkedin");
		System.out.println("(30/07/2024 06:24:38)");
		try {
			carregaProp();
			ObtemOportunidadeLinkedinObj obj = new ObtemOportunidadeLinkedinObj();
			obj.executa();
			LocalDateTime dataHoraAtual = LocalDateTime.now();
			DateTimeFormatter formatador = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
			String dataHoraFormatada = dataHoraAtual.format(formatador);
			System.out.println("finalizou " + dataHoraFormatada);
			System.exit(0);
		} catch (Exception e) {
			gravarErro(e);
		}
	}


	private static void gravarErro(Exception e) {
		try {
			FileWriter fileWriter = new FileWriter("ObtemOportunidadeLinkedin.err", true);
			PrintWriter printWriter = new PrintWriter(fileWriter);
			e.printStackTrace(printWriter);
			printWriter.close();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	private static void carregaProp() throws IOException {
		//System.out.println("Dir:" + System.getProperty("user.dir"));
		//InputStream input = new FileInputStream("CriaPythonTreinoRede.config");
		//Properties prop = new Properties();
		//prop.load(input);
		//UrlLoopback = prop.getProperty("loopback.url");
		UrlLoopback = "http://163.245.200.7:3000/api";
		DaoBaseComum.setUrl(UrlLoopback);
	}

	private static void preparaComum() {
		DaoBaseComum.setUrl(UrlLoopback);
		DaoBaseComum.setProximo("ObtemOportunidadeLinkedinObj", new PalavraRaiz_ListaParaVisitaImpl());
		DaoBaseComum.setProximo("PalavraRaiz_ListaParaVisita", new AcessaLinkedInImpl());
		DaoBaseComum.setProximo("AcessaLinkedIn", new CriaArquivoTextoImpl());
		DaoBaseComum.setProximo("CriaArquivoTexto", new LoopItemImpl());
		DaoBaseComum.setProximo("LoopItem", new OportunidadeLinkedin_RecebeItemImpl());
	}
}
