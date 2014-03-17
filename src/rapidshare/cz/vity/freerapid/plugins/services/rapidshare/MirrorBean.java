package cz.vity.freerapid.plugins.services.rapidshare;


public class MirrorBean {
    String name;
    String ident;

    public void setName(String name) {
        this.name = name;
    }

    public void setIdent(String ident) {
        this.ident = ident;
    }

    public MirrorBean() {
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
  }

