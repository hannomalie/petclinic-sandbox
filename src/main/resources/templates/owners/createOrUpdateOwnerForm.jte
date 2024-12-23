@import org.springframework.samples.petclinic.owner.Owner
@import org.springframework.samples.petclinic.system.Templating.CustomFields
@param CustomFields fields

@param Owner owner

@template.fragments.layout(content = @`
    <h2>Owner</h2>
    <form class="form-horizontal" id="add-owner-form" method="post">
        <div class="form-group has-feedback">
            @template.fragments.inputField(fields = fields, label = "First Name", name = "firstName", value = owner.getFirstName(), type =  "text")
            @template.fragments.inputField(fields = fields, label = "Last Name", name = "lastName", value = owner.getLastName(), type =  "text")
            @template.fragments.inputField(fields = fields, label = "Address", name = "address", value =  owner.getAddress(), type = "text")
            @template.fragments.inputField(fields = fields, label = "City", name = "city", value =  owner.getCity(), type = "text")
            @template.fragments.inputField(fields = fields, label = "Telephone", name = "telephone", value =  owner.getTelephone(), type = "text")
        </div>
        <div class="form-group">
            <div class="col-sm-offset-2 col-sm-10">
                <button class="btn btn-primary" id="submit-owner" type="submit">${owner.isNew() ? "Add " : "Update "}Owner</button>
            </div>
        </div>
    </form>
`, menu = "owners")
