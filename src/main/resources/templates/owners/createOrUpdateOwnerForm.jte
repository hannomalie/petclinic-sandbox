@import org.springframework.samples.petclinic.owner.Owner
@import org.springframework.samples.petclinic.system.Templating.CustomFields
@param CustomFields fields

@param Owner owner

@template.fragments.layout(content = @`
    <h2>Owner</h2>
    <form class="form-horizontal" id="add-owner-form" method="post">
        <div class="form-group has-feedback">
            @template.fragments.inputField(fields, "First Name", "firstName",owner.getFirstName(),  "text")
            @template.fragments.inputField(fields, "Last Name", "lastName",owner.getLastName(),  "text")
            @template.fragments.inputField(fields, "Address", "address", owner.getAddress(), "text")
            @template.fragments.inputField(fields, "City", "city", owner.getCity(), "text")
            @template.fragments.inputField(fields, "Telephone", "telephone", owner.getTelephone(), "text")
        </div>
        <div class="form-group">
            <div class="col-sm-offset-2 col-sm-10">
                <button class="btn btn-primary" type="submit">${owner.isNew() ? "Add " : "Update "}Owner</button>
            </div>
        </div>
    </form>
`, menu = "owners")
