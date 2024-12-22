@import org.springframework.samples.petclinic.system.Templating.CustomFields
@import java.util.stream.Collectors
@param CustomFields fields
@param org.springframework.samples.petclinic.owner.Pet pet
@param org.springframework.samples.petclinic.owner.Owner owner
@param java.util.List<org.springframework.samples.petclinic.owner.PetType> types

@template.fragments.layout(content = @`
    <h2>
        @if(pet.isNew())
            New Pet
        @else
            Pet
        @endif
    </h2>
    <form class="form-horizontal" method="post">
        <input type="hidden" name="id" value="${pet.getId()}" />
        <div class="form-group has-feedback">
            <div class="form-group">
                <label class="col-sm-2 control-label">Owner</label>
                <div class="col-sm-10">
                    <span>${owner == null ? " " : (owner.getFirstName() + " " + owner.getLastName())}</span>
                </div>
            </div>
            @template.fragments.inputField(fields = fields, label = "Name", name = "name", value = pet.getName(), type = "text")
            @template.fragments.inputField(fields = fields, label = "Birth Date", name = "birthDate", value = pet.getBirthDate().toString(), "date")
            !{var typeString = pet.getType() == null ? "" : pet.getType().toString(); }
            @template.fragments.selectField(fields = fields, label = "Type", name = "type", value = typeString, items = types.stream().map(it -> it.getName()).collect(Collectors.toUnmodifiableList()))
        </div>
        <div class="form-group">
            <div class="col-sm-offset-2 col-sm-10">
                <button class="btn btn-primary" type="submit">
                    @if(pet.isNew())
                        Add Pet
                    @else
                        Update Pet
                    @endif
                </button>
            </div>
        </div>
    </form>
`, menu = "owners")
