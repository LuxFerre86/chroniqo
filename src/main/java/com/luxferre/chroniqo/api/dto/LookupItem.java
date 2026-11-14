package com.luxferre.chroniqo.api.dto;

/**
 * A selectable code/name pair, used for countries and their subdivisions.
 *
 * @param code ISO code
 * @param name display name
 */
public record LookupItem(String code, String name) {
}
