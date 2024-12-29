@import org.springframework.samples.petclinic.owner.Pet
@import org.springframework.samples.petclinic.owner.Visit
@import java.time.format.DateTimeFormatter
@param String message
@param String error
@param org.springframework.samples.petclinic.owner.Owner owner
@param java.util.List<Pet> pets
@param java.util.Map<Pet, java.util.List<Visit>> visits

!{var hasMessage = message != null && !message.isEmpty();}
!{var hasError = error != null && !error.isEmpty();}
@template.fragments.layout(content = @`
    <h2>Owner Information</h2>

    @if(hasMessage)
        <div class="alert alert-success" id="success-message">
            <span>${message}</span>
        </div>
    @endif

    @if(hasError)
        <div class="alert alert-danger" id="error-message">
            <span>${error}</span>
        </div>
    @endif




    <table class="table table-striped">
        <tr>
            <th>Name</th>
            <td><b>${owner.getFirstName() + " " + owner.getLastName()}</b></td>
        </tr>
        <tr>
            <th>Address</th>
            <td>${owner.getAddress()}</td>
        </tr>
        <tr>
            <th>City</th>
            <td>${owner.getCity()}</td>
        </tr>
        <tr>
            <th>Telephone</th>
            <td>${owner.getTelephone()}</td>
        </tr>
    </table>

    <a href="/owners/${owner.getId()}/edit" id="edit-owner" class="btn btn-primary">Edit Owner</a>
    <a href="/owners/${owner.getId()}/pets/new" id="add-pet-to-owner" class="btn btn-primary">Add New Pet</a>

    <br />
    <br />
    <br />
    <h2>Pets and Visits</h2>

    <table class="table table-striped">

        @for(Pet pet: pets)
            <tr>
                <td valign="top">
                    <dl class="dl-horizontal">
                        <dt>Name</dt>
                        <dd>${pet.getName()}</dd>
                        <dt>Birth Date</dt>
                        <dd>${DateTimeFormatter.ofPattern("yyyy-MM-dd").format(pet.getBirthDate())}</dd>
                        <dt>Type</dt>
                        <dd>${pet.getType() == null ? "" : pet.getType().getName()}</dd>
                    </dl>
                </td>
                <td valign="top">
                    <table class="table-condensed">
                        <thead>
                        <tr>
                            <th>Visit Date</th>
                            <th>Description</th>
                        </tr>
                        </thead>
                        @if(visits.get(pet) != null)
                            @for(Visit visit: visits.get(pet))
                                <tr>
                                    <td>${DateTimeFormatter.ofPattern("yyyy-MM-dd").format(visit.getDate())}</td>
                                    <td>${visit.getDate().toString()}</td>
                                    <td>${visit == null ? "" : visit.getDescription()}</td>
                                </tr>
                            @endfor
                        @endif
                    </table>
                </td>
            </tr>
            <tr>
                <td><a href="/owners/${owner.getId()}/pets/${pet.getId()}/edit" id="edit-pet">Edit Pet</a></td>
                <td><a href="/owners/${owner.getId()}/pets/${pet.getId()}/visits/new" id="add-visit-to-pet">Add Visit</a></td>
            </tr>
        @endfor

    </table>
    <script>
        // Function to hide the success and error messages after 3 seconds
        function hideMessages() {
            setTimeout(function() {
                document.getElementById("success-message").style.display = "none";
                document.getElementById("error-message").style.display = "none";
            }, 3000); // 3000 milliseconds (3 seconds)
        }

        // Call the function to hide messages
        hideMessages();
    </script>
`, menu = "owners")
