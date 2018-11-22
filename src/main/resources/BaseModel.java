import br.com.softmovel.banco.SivaSysWssEmpresas;
import io.ebean.annotation.WhenCreated;
import io.ebean.annotation.WhenModified;
import io.ebean.annotation.WhoCreated;
import io.ebean.annotation.WhoModified;

import javax.persistence.*;
import java.time.Instant;

@MappedSuperclass
public class BaseModel {


    @JoinColumn(name = "codigo_empresa", referencedColumnName = "codigo_empresa")
    @ManyToOne(fetch = FetchType.LAZY)
    private SivaSysWssEmpresas sivaSysWssEmpresas;

    @Column(name = "cod_lancamento", nullable = false, length=50)
    private String codLancamento;


    @WhenCreated
    @Column(name = "data_insercao", nullable = false)
    private Instant dataInsercao;

    @WhenModified
    @Column(name = "data_modificacao", nullable = false)
    private Instant dataModificacao;

    @WhoCreated
    @Column(name = "user_insercao", nullable = false)
    private int userInsercao;

    @WhoModified
    @Column(name = "user_modificacao", nullable = false)
    private int userModificacao;

    @Column(name = "flg_ativo", nullable = false)
    private int flgAtivo;

    @Version
    private long version;


    public SivaSysWssEmpresas getSivaSysWssEmpresas() {
        return sivaSysWssEmpresas;
    }

    public void setSivaSysWssEmpresas(SivaSysWssEmpresas sivaSysWssEmpresas) {
        this.sivaSysWssEmpresas = sivaSysWssEmpresas;
    }

    public String getCodLancamento() {
        return codLancamento;
    }

    public void setCodLancamento(String codLancamento) {
        this.codLancamento = codLancamento;
    }

    public Instant getDataInsercao() {
        return dataInsercao;
    }

    public void setDataInsercao(Instant dataInsercao) {
        this.dataInsercao = dataInsercao;
    }

    public Instant getDataModificacao() {
        return dataModificacao;
    }

    public void setDataModificacao(Instant dataModificacao) {
        this.dataModificacao = dataModificacao;
    }

    public int getUserInsercao() {
        return userInsercao;
    }

    public void setUserInsercao(int userInsercao) {
        this.userInsercao = userInsercao;
    }

    public int getUserModificacao() {
        return userModificacao;
    }

    public void setUserModificacao(int userModificacao) {
        this.userModificacao = userModificacao;
    }

    public int getFlgAtivo() {
        return flgAtivo;
    }

    public void setFlgAtivo(int flgAtivo) {
        this.flgAtivo = flgAtivo;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }
}
