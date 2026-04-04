package br.com.gersis.daobase;

public class DummyDaoBase extends DaoBase {
	
	public DummyDaoBase() {
		super();
		this.setConcluido();
	}
	
	

	@Override
	public void executaProximo() {
		this.finalizar();
	}
	@Override
	public void executa() {
		this.finalizar();
	}



	@Override
	protected void executaImpl() {

	}

	@Override
	protected DaoBase getProximo() {
		return null;
	}



	@Override
	protected long getTempo() {
		return 2000;
	}



	@Override
	protected int getNumPasso() {
		return 0;
	}

	@Override
	protected IDatasetComum criaDataSet() {
		return null;
	}

}
