@import org.springframework.samples.petclinic.owner.Pet
@import org.springframework.samples.petclinic.owner.Owner
@import java.util.stream.Collectors
@param java.util.List<Owner> owners
@param int totalPages
@param int currentPage
@param java.util.HashMap<Integer, java.util.List<Pet>> petsForOwnerId
@template.fragments.layout(content = @`
    <h2>Owners</h2>l

    <table id="owners" class="table table-striped">
        <thead>
        <tr>
            <th style="width: 150px;">Name</th>
            <th style="width: 200px;">Address</th>
            <th>City</th>
            <th style="width: 120px">Telephone</th>
            <th>Pets</th>
        </tr>
        </thead>
        <tbody>
        @for(Owner owner : owners)
            <tr>
                <td>
                    <a href="/owners/${owner.getId()}">${owner.getFirstName() + " " + owner.getLastName()}</a>
                </td>
                <td>${owner.getAddress()}</td>
                <td>${owner.getCity()}</td>
                <td>${owner.getTelephone()}</td>
                !{var pets = petsForOwnerId.get(owner.getId());}
                <td><span>${pets == null ? "" : pets.stream().map(it -> it.toString()).collect(Collectors.joining(", "))}</span></td>
            </tr>
        @endfor
        </tbody>
    </table>
    @if(totalPages > 1)
        <div>
            <span>Pages:</span>
            <span>[</span>

            @for(int i = 0; i < totalPages; ++i)
                <span>${i}</span>
                @if(!(currentPage != 1))
                    <a href="/owners?page='${i}}">[[${i}]]</a>
                @endif
                <span>]&nbsp;</span>
                <span>
                    @if(currentPage > 1)
                        <a href="/owners?page=1" title="First" class="fa fa-fast-backward"></a>
                    @else
                        <span title="First" class="fa fa-fast-backward"></span>
                    @endif
                </span>
                <span>
                    @if(currentPage > 1)
                        <a href="/owners?page=${currentPage - 1}" title="Previous"
                           class="fa fa-step-backward"></a>
                    @else
                        <span title="Previous" class="fa fa-step-backward"></span>
                    @endif
                </span>
                <span>
                    @if(currentPage < totalPages)
                        <a href="/owners?page=${currentPage + 1}}" title="Next"
                           class="fa fa-step-forward"></a>
                    @endif
                </span>
                <span>
                    @if(currentPage < totalPages)
                        <a href="/owners?page=${totalPages}}" title="Last"
                           class="fa fa-fast-forward"></a>
                    @else
                        <span title="Last" class="fa fa-step-forward"></span>
                    @endif
                </span>
            @endfor
        </div>
    @endif
`, menu = "owners")

