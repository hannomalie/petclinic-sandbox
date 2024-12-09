@import org.springframework.samples.petclinic.vet.Specialty
@import org.springframework.samples.petclinic.vet.Vet
@param java.util.List<Vet> vets
@param int totalPages
@param int currentPage

@template.fragments.layout(content = @`
    <h2>Veterinarians</h2>

    <table id="vets" class="table table-striped">
        <thead>
        <tr>
            <th>Name</th>
            <th>Specialties</th>
        </tr>
        </thead>
        <tbody>
        @for(Vet vet: vets)
            <tr>
                <td>${vet.getFirstName() + ' ' + vet.getLastName()}</td>
                <td>
                    @for(Specialty specialty: vet.getSpecialties())
                        <span>${specialty.getName() + ' '}</span>
                    @endfor
                    @if(vet.getSpecialties().isEmpty())
                        <span>none</span>
                    @endif
                </td>
            </tr>
        @endfor
        </tbody>
    </table>


    @if(totalPages > 1)
        <div>
            <span>Pages:</span>
            <span>[</span>
            @for(int i = 1; i < totalPages; i++)
                <span>
                    @if(currentPage != i)
                        <a href="/vets?page=__${i}__'}">[[${i}]]</a>
                    @endif
                </span>
            @endfor
            <span>] </span>
            <span>
                @if(currentPage > 1)
                    <a href="/vets?page=1" title="First" class="fa fa-fast-backward"></a>
                @else
                    <span title="First" class="fa fa-fast-backward"></span>
                @endif
            </span>
            <span>
                @if(currentPage > 1)
                    <a href="/vets?page=${currentPage - 1}" title="Previous" class="fa fa-step-backward"></a>
                @else
                    <span title="Previous" class="fa fa-step-backward"></span>
                @endif
            </span>
            <span>
                @if(currentPage < totalPages)
                    <a href="/vets?page=${currentPage + 1}" title="Next" class="fa fa-step-forward"></a>
                @else
                    <span title="Next" class="fa fa-step-forward"></span>
                @endif
            </span>
            <span>
                @if(currentPage < totalPages)
                    <a href="/vets?page=${totalPages}" title="Last" class="fa fa-fast-forward"></a>
                @else
                    <span title="Last" class="fa fa-fast-forward"></span>
                @endif
            </span>
        </div>
    @endif
`, menu = "vets")
