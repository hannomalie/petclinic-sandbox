package org.springframework.samples.petclinic.owner;

import java.util.List;

public record OwnerAndPets(Owner owner, List<Pet> pets) {
}
