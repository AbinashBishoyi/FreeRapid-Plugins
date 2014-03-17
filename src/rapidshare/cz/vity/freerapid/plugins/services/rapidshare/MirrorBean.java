package cz.vity.freerapid.plugins.services.rapidshare;


public class MirrorBean {
    private String name;
    private String ident;

    public MirrorBean() {

    }

    public MirrorBean(String name, String ident) {
        this.name = name;
        this.ident = ident;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setIdent(String ident) {
        this.ident = ident;
    }

    public String getName() {
        return name;
    }

    public String getIdent() {
        return ident;
    }

    public String toString() {
        return name;
    }


    public static MirrorBean createDefault() {
        return new MirrorBean("default", "default");
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MirrorBean that = (MirrorBean) o;

        if (ident != null ? !ident.equals(that.ident) : that.ident != null) return false;

        return true;
    }

    public int hashCode() {
        return (ident != null ? ident.hashCode() : 0);
    }
}

