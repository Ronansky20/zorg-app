"use client"

import {
  DisclosurePanel as CollapsibleContentPrimitive,
  Disclosure as CollapsiblePrimitive,
  Button as CollapsibleTriggerPrimitive,
} from "react-aria-components";

function Collapsible({
  ...props
}) {
  return <CollapsiblePrimitive data-slot="collapsible" {...props} />
}

function CollapsibleTrigger({
  ...props
}) {
  return (
    <CollapsibleTriggerPrimitive
      slot="trigger"
      data-slot="collapsible-trigger"
      {...props}
    />
  )
}

function CollapsibleContent({
  ...props
}) {
  return (
    <CollapsibleContentPrimitive data-slot="collapsible-content" {...props} />
  )
}

export { Collapsible, CollapsibleTrigger, CollapsibleContent }
