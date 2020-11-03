package nl.finalist.liferay.oidc;

public class AzureADGroup {
    private String name;
    private String objectId;
    private String description;
    private String email;

    private AzureADGroup(AzureADGroupBuilder builder) {
        this.setName(builder.name);
        this.setObjectId(builder.objectId);
        this.setDescription(builder.description);
        this.setEmail(builder.mail);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getObjectId() {
        return objectId;
    }

    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public static class AzureADGroupBuilder {
        private String name;
        private String objectId;
        private String description;
        private String mail;

        public AzureADGroupBuilder(String name) {
            this.name = name;
        }

        public AzureADGroupBuilder objectId(String objectId) {
            this.objectId = objectId;
            return this;
        }
        public AzureADGroupBuilder description(String description) {
            this.description = description;
            return this;
        }
        public AzureADGroupBuilder mail(String mail) {
            this.mail = mail;
            return this;
        }

        public AzureADGroup build() {
            AzureADGroup azureADGroup =  new AzureADGroup(this);
            return azureADGroup;
        }
    }
}
