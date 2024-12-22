@import org.springframework.samples.petclinic.owner.PetType
@import org.springframework.samples.petclinic.owner.Visit
@import org.springframework.samples.petclinic.system.Templating.CustomFields
@import org.springframework.validation.FieldError

@param CustomFields fields
@param org.springframework.samples.petclinic.owner.Visit visit
@param org.springframework.samples.petclinic.owner.Pet pet
@param org.springframework.samples.petclinic.owner.Owner owner
@param java.util.HashMap<Integer, java.util.List<Visit>> visitsForPetId

@template.fragments.layout(content = @`
<h2>
    @if(visit.isNew())
        New Visit
    @else
        Visit
    @endif
</h2>

<b>Pet</b>
<table class="table table-striped">
    <thead>
    <tr>
        <th>Name</th>
        <th>Birth Date</th>
        <th>Type</th>
        <th>Owner</th>
    </tr>
    </thead>
    <tr>
        <td>${pet.getName()}</td>
        <td>${java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd").format(pet.getBirthDate())}</td>
        <td>${pet.getType().getName()}</td>
        <td>${owner == null ? " " : owner.getFirstName() + " " + owner.getLastName()}</td>
    </tr>
</table>

<form class="form-horizontal" method="post">
    <div class="form-group has-feedback">
            @template.fragments.inputField(fields = fields, label = "Date", name = "date", value = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd").format(visit.getDate()), type = "date")
            @template.fragments.inputField(fields = fields, label = "Description", name = "description", value = visit.getDescription(), type = "text")
    </div>

    <div class="form-group">
        <div class="col-sm-offset-2 col-sm-10">
            <input type="hidden" name="petId" value="${pet.getId()}" />
            <button class="btn btn-primary" type="submit">Add Visit</button>
        </div>
    </div>
</form>

<br />
<b>Previous Visits</b>
<table class="table table-striped">
    <tr>
        <th>Date</th>
        <th>Description</th>
    </tr>

    @if(!visit.isNew())
        @for(Visit visit : visitsForPetId.get(pet.getId()))
            <tr>
                <td>${java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd").format(visit.getDate())}</td>
                <td>${visit.getDescription()}</td>
            </tr>
        @endfor
    @endif
</table>
`, menu = "visits")
